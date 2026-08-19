package com.printnow.module.order.service;

import com.printnow.infrastructure.email.EmailService;
import com.printnow.module.order.dto.CommandeRequestDTO;
import com.printnow.module.order.dto.CommandeResponseDTO;
import com.printnow.module.order.dto.LigneCommandeRequestDTO;
import com.printnow.module.order.enums.ModeRetrait;
import com.printnow.module.order.enums.StatutCommande;
import com.printnow.module.order.enums.StatutLivraison;
import com.printnow.module.order.mapper.AdresseLivraisonMapper;
import com.printnow.module.order.mapper.CommandeMapper;

import com.printnow.module.order.model.*;
import com.printnow.module.order.repository.CommandeRepository;
import com.printnow.module.promo.enums.TypeReduction;
import com.printnow.module.promo.model.CodePromo;
import com.printnow.module.etudiant.enums.StatutEtudiant;
import com.printnow.module.etudiant.repository.VerificationEtudiantRepository;
import com.printnow.module.promo.service.CodePromoService;
import com.printnow.module.shop.enums.TypePlastification;
import com.printnow.module.shop.enums.TypeProduit;
import com.printnow.module.shop.enums.TypeReliure;
import com.printnow.module.shop.model.Imprimerie;
import com.printnow.module.shop.model.Produit;
import com.printnow.module.shop.repository.ProduitRepository;
import com.printnow.module.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.printnow.module.correction.service.CorrectionCommandeService;
import com.printnow.module.studio.service.StudioCommandeService;
import com.printnow.module.parametres.service.ParametresService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommandeService {

    private static final BigDecimal CENTIMES = new BigDecimal("100");

    /** Un centime d'écart reste imputable aux arrondis, pas à un désaccord de calcul. */
    private static final long TOLERANCE_CENTIMES = 1;

    private final CommandeRepository commandeRepository;
    private final EmailService emailService;
    private final ProduitRepository produitRepository;
    private final CommandeMapper commandeMapper;
    private final AdresseLivraisonMapper adresseLivraisonMapper;
    private final CodePromoService codePromoService;
    private final VerificationEtudiantRepository verificationEtudiantRepository;
    private final CorrectionCommandeService correctionCommandeService;
    private final StudioCommandeService studioCommandeService;
    private final ParametresService parametresService;

    /**
     * Passe une commande et applique, dans la même transaction, les vérifications
     * orthographiques réglées avec elle.
     *
     * Les deux opérations sont indissociables. Le contrôle du paiement des
     * corrections intervient après le calcul du total de l'impression, dont il a
     * besoin ; s'il échoue, la commande ne doit pas subsister pour autant. Sans
     * cette transaction commune, un règlement insuffisant laissait en base une
     * commande orpheline, payée à moitié et jamais honorée.
     *
     * @param montantRegle montant réellement encaissé par Stripe, en centimes
     */
    @Transactional
    public CommandeResponseDTO passerCommande(CommandeRequestDTO request, User client,
                                              boolean paiementConfirme, Long montantRegle) {
        CommandeResponseDTO commande = createCommande(request, client, paiementConfirme);

        BigDecimal montantCorrections = correctionCommandeService.appliquerCorrections(
                request.getCorrections(), client, commande.getTotalTTC(), montantRegle);

        // Designs générés par l'IA (studio) utilisés dans la commande : forfait au
        // bénéfice de PrintNow, comme les corrections. Le contrôle est cumulé :
        // le paiement doit couvrir impression + corrections + générations.
        BigDecimal montantGenerations = studioCommandeService.appliquerGenerations(
                request.getGenerations(), client, commande.getTotalTTC().add(montantCorrections), montantRegle);

        // Ces montants sont des revenus de la plateforme, jamais de l'imprimerie :
        // on les conserve à part, sans jamais les ajouter au total de la commande
        // ni à l'assiette de la commission.
        if (montantCorrections.signum() > 0) {
            commande = enregistrerMontantCorrections(commande.getId(), montantCorrections);
        }
        if (montantGenerations.signum() > 0) {
            commande = enregistrerMontantGenerations(commande.getId(), montantGenerations);
        }

        if (paiementConfirme) verifierMontantRegle(commande, montantCorrections.add(montantGenerations), montantRegle);
        return commande;
    }

    /**
     * Compare la somme encaissée au total que le serveur vient de calculer.
     *
     * Le montant débité est décidé par le navigateur : tant que personne ne les
     * confronte, un désaccord entre les deux calculs passe inaperçu — c'est
     * ainsi que les options de reliure et de finition ont pu être facturées aux
     * clients sans jamais figurer dans les commandes.
     *
     * @throws RuntimeException si le règlement ne couvre pas la commande ; le
     *         contrôleur la refuse alors et rembourse.
     */
    private void verifierMontantRegle(CommandeResponseDTO commande, BigDecimal supplementsPlateforme, Long montantRegle) {
        if (montantRegle == null) return;

        long duEnCentimes = commande.getTotalTTC().add(supplementsPlateforme)
                .multiply(CENTIMES).setScale(0, RoundingMode.HALF_UP).longValue();
        long ecart = montantRegle - duEnCentimes;

        if (ecart < -TOLERANCE_CENTIMES) {
            throw new RuntimeException(String.format(
                    "Le paiement de %.2f € ne couvre pas cette commande (%.2f € dus).",
                    montantRegle / 100.0, duEnCentimes / 100.0));
        }
        if (ecart > TOLERANCE_CENTIMES) {
            // On n'annule pas : la commande est payée et honorable. Mais l'écart
            // signale un calcul divergent entre le navigateur et le serveur.
            log.error("Commande {} : {} centimes encaissés de trop ({} attendus). "
                            + "Les deux calculs de prix divergent — à corriger avant que d'autres clients ne surpaient.",
                    commande.getNumeroCommande(), ecart, duEnCentimes);
        }
    }

    /**
     * Crée une nouvelle commande avec calcul des prix, taxes, livraison et commissions
     */
    @Transactional
    public CommandeResponseDTO createCommande(CommandeRequestDTO request, User client, boolean paiementConfirme) {
        Commande commande = new Commande();
        commande.setNumeroCommande(generateOrderNumber());
        commande.setDateCreation(LocalDateTime.now());
        commande.setClient(client);

        // Le paiement Stripe a déjà été vérifié par le contrôleur : la commande
        // part directement en PAYEE, sinon elle attend son règlement.
        if (paiementConfirme) {
            commande.setStatut(StatutCommande.PAYEE);
            commande.setDatePaiement(LocalDateTime.now());
            commande.setPaymentIntentId(request.getPaymentIntentId());
        } else {
            commande.setStatut(StatutCommande.EN_ATTENTE_PAIEMENT);
        }
        
        // 1. Gestion du mode de retrait et de la livraison
        boolean isLivraison = false;
        if (request.getModeRetrait() != null) {
            ModeRetrait mode = ModeRetrait.valueOf(request.getModeRetrait());
            commande.setModeRetrait(mode);
            // Vérifie si le mode correspond à une livraison à domicile
            if (mode == ModeRetrait.LIVRAISON ) { 
                isLivraison = true;
            }
        }

        // 2. Création de l'adresse et de la livraison si nécessaire
        if (isLivraison && request.getAdresseLivraison() != null) {
            // Création de l'entité AdresseLivraison via le Mapper ! (1 seule ligne)
            AdresseLivraison adresse = adresseLivraisonMapper.toEntity(request.getAdresseLivraison());
            commande.setAdresseLivraison(adresse); // Enregistré automatiquement grâce au CascadeType.ALL

            // Initialisation du suivi de Livraison
            Livraison livraison = Livraison.builder()
                    .statutLivraison(StatutLivraison.EN_PREPARATION)
                    .commande(commande) // Liaison bidirectionnelle
                    .build();
            commande.setLivraison(livraison);
        }

        // 3. Gestion de l'option Express 2h
        boolean isExpress = request.getExpress2h() != null && request.getExpress2h();
        if (isExpress) {
            // Récupérer l'imprimerie via le premier produit commandé
            Long premierProduitId = request.getLignes().get(0).getProduitId();
            Produit produit = produitRepository.findById(premierProduitId)
                    .orElseThrow(() -> new RuntimeException("Produit non trouvé"));
            var horaires = produit.getImprimerie().getHoraires();

            Map<DayOfWeek, String> joursMap = Map.of(
                DayOfWeek.MONDAY, "LUNDI", DayOfWeek.TUESDAY, "MARDI",
                DayOfWeek.WEDNESDAY, "MERCREDI", DayOfWeek.THURSDAY, "JEUDI",
                DayOfWeek.FRIDAY, "VENDREDI", DayOfWeek.SATURDAY, "SAMEDI",
                DayOfWeek.SUNDAY, "DIMANCHE"
            );
            String jourAujourdhui = joursMap.get(LocalDateTime.now().getDayOfWeek());

            LocalTime maintenant = LocalTime.now();
            boolean disponible = horaires != null && horaires.stream().anyMatch(h ->
                jourAujourdhui.equals(h.getJourSemaine()) &&
                (h.getFerme() == null || !h.getFerme()) &&
                h.getHeureOuverture() != null &&
                h.getHeureFermeture() != null &&
                maintenant.isAfter(h.getHeureOuverture()) &&
                maintenant.plusHours(2).isBefore(h.getHeureFermeture())
            );

            if (!disponible) {
                throw new RuntimeException("L'option express 2h n'est pas disponible : l'imprimerie ferme dans moins de 2 heures ou est fermée aujourd'hui.");
            }
        }
        commande.setExpress2h(isExpress);

        if (request.getLignes() == null || request.getLignes().isEmpty()) {
            throw new RuntimeException("La commande doit contenir au moins un article.");
        }

        // On récupère l'imprimerie via le premier produit de la liste
        Produit premierProduit = produitRepository.findById(request.getLignes().get(0).getProduitId())
                .orElseThrow(() -> new RuntimeException("Produit non trouvé"));
        commande.setImprimerie(premierProduit.getImprimerie());

        BigDecimal totalHT = BigDecimal.ZERO;

        // 4. Boucle sur chaque fichier/produit commandé
        for (LigneCommandeRequestDTO item : request.getLignes()) {
            Produit produit = produitRepository.findById(item.getProduitId())
                    .orElseThrow(() -> new RuntimeException("Produit non trouvé ID: " + item.getProduitId()));
            
            BigDecimal prixBase = BigDecimal.valueOf(produit.getPrixBase() != null ? produit.getPrixBase() : 0.0);

            // Calcul du prix unitaire : Prix de base * Nombre de pages
            BigDecimal nbPages = BigDecimal.valueOf(item.getNbPages() != null && item.getNbPages() > 0 ? item.getNbPages() : 1);
            BigDecimal prixU = prixBase.multiply(nbPages);

            // Le recto-verso n'a aucun sens pour une affiche (personne ne voit le verso) ni
            // pour un document d'une seule page (pas de verso à imprimer) : on l'ignore
            // côté serveur quel que soit ce qu'envoie le client, pour ne pas dépendre
            // uniquement de la restriction faite côté frontend.
            boolean rectoVerso = Boolean.TRUE.equals(item.getRectoVerso())
                    && produit.getTypeProduit() != TypeProduit.POSTER
                    && nbPages.compareTo(BigDecimal.ONE) > 0;

            // Remise recto-verso (configurée par l'imprimerie, 15% par défaut) : ne
            // s'applique qu'au coût d'impression, pas à la reliure/finition ci-dessous.
            if (rectoVerso) {
                Integer pourcentageRV = produit.getImprimerie().getPourcentageRemiseRectoVerso();
                BigDecimal tauxRV = BigDecimal.valueOf(pourcentageRV != null ? pourcentageRV : 15);
                BigDecimal remiseRV = prixU.multiply(tauxRV).divide(new BigDecimal("100"));
                prixU = prixU.subtract(remiseRV).max(BigDecimal.ZERO);
            }

            // Reliure et finition sont indexées par leur énumération, pas par leur
            // nom : chercher avec la chaîne reçue renvoyait toujours null, et ces
            // options étaient facturées au client sans jamais entrer dans le total.
            prixU = prixU.add(prixOption(produit.getPrixParTypeReliure(),
                    item.getReliure(), TypeReliure.class, "reliure"));
            prixU = prixU.add(prixOption(produit.getPrixParTypePlastification(),
                    item.getFinition(), TypePlastification.class, "finition"));

            // Construction de la ligne de commande
            LigneCommande ligne = LigneCommande.builder()
                    .produit(produit)
                    .quantite(item.getQuantite())
                    .nbPages(item.getNbPages())
                    .couleur(item.getCouleur())
                    .rectoVerso(rectoVerso)
                    .reliure(item.getReliure()) 
                    .finition(item.getFinition())
                    .prixUnitaire(prixU)
                    .prixTotal(prixU.multiply(BigDecimal.valueOf(item.getQuantite())))
                    .build();

            commande.addLigneCommande(ligne);
            totalHT = totalHT.add(ligne.getPrixTotal());
        }

        // 5. Frais express (prix configuré par l'imprimeur)
        if (isExpress) {
            Imprimerie imp = commande.getImprimerie();
            BigDecimal prixExpress = (imp.getPrixExpress2h() != null)
                    ? BigDecimal.valueOf(imp.getPrixExpress2h())
                    : new BigDecimal("5.00");
            totalHT = totalHT.add(prixExpress);
            commande.setFraisExpress(prixExpress);
        }

        // 6. Frais de livraison (prix configuré par l'imprimeur, 4.99€ par défaut)
        if (isLivraison) {
            Imprimerie imp = commande.getImprimerie();
            BigDecimal prixLivraison = (imp.getPrixLivraison() != null)
                    ? BigDecimal.valueOf(imp.getPrixLivraison())
                    : new BigDecimal("4.99");
            totalHT = totalHT.add(prixLivraison);
            commande.setFraisLivraison(prixLivraison);
        }

        // 7. Remise étudiant
        if (Boolean.TRUE.equals(request.getTarifEtudiant())) {
            Imprimerie imp = commande.getImprimerie();
            if (!Boolean.TRUE.equals(imp.getProposeTarifEtudiant()) || imp.getPourcentageRemiseEtudiant() == null) {
                throw new RuntimeException("Cette imprimerie ne propose pas de tarif étudiant.");
            }
            boolean verifie = verificationEtudiantRepository.findByUser_Id(client.getId())
                    .map(v -> v.getStatut() == StatutEtudiant.ACCEPTE
                            && v.getValableJusquA() != null
                            && java.time.LocalDateTime.now().isBefore(v.getValableJusquA()))
                    .orElse(false);
            if (!verifie) {
                throw new RuntimeException("Votre statut étudiant n'est pas vérifié ou a expiré.");
            }
            BigDecimal remise = totalHT.multiply(
                    BigDecimal.valueOf(imp.getPourcentageRemiseEtudiant()).divide(new BigDecimal("100")));
            totalHT = totalHT.subtract(remise).max(BigDecimal.ZERO);
            commande.setMontantReductionEtudiant(remise);
        }

        // 8. Application du code promo
        BigDecimal montantReduction = BigDecimal.ZERO;
        if (request.getCodePromo() != null && !request.getCodePromo().isBlank()) {
            BigDecimal totalTTCAvantPromo = totalHT.multiply(new BigDecimal("1.20"));
            CodePromo promo = codePromoService.appliquerCode(request.getCodePromo(), totalTTCAvantPromo,
                    client.getId(), commande.getImprimerie() != null ? commande.getImprimerie().getId() : null);
            if (promo.getTypeReduction() == TypeReduction.POURCENTAGE) {
                montantReduction = totalHT.multiply(promo.getValeurReduction()).divide(new BigDecimal("100"));
            } else {
                montantReduction = promo.getValeurReduction();
            }
            totalHT = totalHT.subtract(montantReduction).max(BigDecimal.ZERO);
            commande.setCodePromo(promo);
            commande.setMontantReduction(montantReduction);
        }

        // 8. Calculs financiers finaux
        commande.setTotalHT(totalHT);
        commande.setTotalTVA(totalHT.multiply(new BigDecimal("0.21"))); // TVA fixe à 21% (taux belge standard)
        commande.setTotalTTC(commande.getTotalHT().add(commande.getTotalTVA()));
        
        // Commission, en pourcentage configurable du TTC
        BigDecimal tauxCommission = parametresService.getParametres().getCommissionPourcentage();
        BigDecimal commission = commande.getTotalTTC()
                .multiply(tauxCommission)
                .divide(new BigDecimal("100"));
        commande.setCommissionPlateforme(commission);
        // Conservé tel quel : recalculer ce taux plus tard depuis le montant
        // arrondi de la commission dériverait sur les petites commandes.
        commande.setCommissionPourcentageAppliquee(tauxCommission);
        
        // Montant net pour l'imprimeur
        commande.setMontantVerseImprimerie(commande.getTotalTTC().subtract(commission));

        // 8. Sauvegarde en base (sauvegarde la commande, ses lignes, l'adresse et le suivi de livraison !)
        Commande savedCommande = commandeRepository.save(commande);

        // Une commande non encore payée peut ne jamais aboutir : on n'alerte
        // l'imprimerie qu'une fois le règlement confirmé.
        if (paiementConfirme) {
            notifierNouvelleCommande(savedCommande);
        }

        return commandeMapper.toDto(savedCommande);
    }

    /**
     * Alerte l'imprimerie par email qu'une commande vient d'être payée.
     *
     * Envoyé à l'email de contact de la boutique (modifiable indépendamment du
     * compte de connexion du gérant), avec le compte du gérant en repli si elle
     * n'est pas renseignée.
     */
    private void notifierNouvelleCommande(Commande commande) {
        Imprimerie imprimerie = commande.getImprimerie();
        String destinataire = (imprimerie.getEmailContact() != null && !imprimerie.getEmailContact().isBlank())
                ? imprimerie.getEmailContact()
                : (imprimerie.getGerant() != null ? imprimerie.getGerant().getEmail() : null);
        if (destinataire == null || destinataire.isBlank()) return;

        String libelleMode = commande.getModeRetrait() == ModeRetrait.LIVRAISON
                ? "Livraison à domicile" : "Retrait en magasin";

        emailService.envoyerNouvelleCommande(
                destinataire,
                commande.getNumeroCommande(),
                libelleMode,
                Boolean.TRUE.equals(commande.getExpress2h()),
                String.format("%.2f €", commande.getTotalTTC()));
    }

    /**
     * Rattache à la commande le montant des vérifications orthographiques réglées
     * avec elle.
     *
     * Ce montant est laissé hors du total, de la commission et de la somme versée
     * à l'imprimerie : la correction est un service de la plateforme, dont le
     * produit lui revient en entier. On l'enregistre uniquement pour qu'il puisse
     * être comptabilisé.
     */
    @Transactional
    public CommandeResponseDTO enregistrerMontantCorrections(Long commandeId, BigDecimal montant) {
        Commande commande = commandeRepository.findById(commandeId)
                .orElseThrow(() -> new RuntimeException("Commande introuvable : " + commandeId));

        commande.setMontantCorrections(montant);
        return commandeMapper.toDto(commandeRepository.save(commande));
    }

    /**
     * Rattache à la commande le montant des designs générés par l'IA réglés avec
     * elle. Comme les corrections, ce montant reste hors du total, de la commission
     * et de la part imprimerie : c'est un revenu de la plateforme, enregistré ici
     * uniquement pour être comptabilisé.
     */
    @Transactional
    public CommandeResponseDTO enregistrerMontantGenerations(Long commandeId, BigDecimal montant) {
        Commande commande = commandeRepository.findById(commandeId)
                .orElseThrow(() -> new RuntimeException("Commande introuvable : " + commandeId));

        commande.setMontantGenerations(montant);
        return commandeMapper.toDto(commandeRepository.save(commande));
    }

    /**
     * Génère un numéro de commande unique (ex: CMD-20250510-A1B2C)
     */
    private String generateOrderNumber() {
        return "CMD-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) 
               + "-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
    }

    /**
     * Récupère l'historique des commandes pour une imprimerie (Dashboard Pro)
     */
    public List<CommandeResponseDTO> getCommandesForImprimerie(Long imprimerieId) {
        return commandeRepository.findParImprimeriePourTableauDeBord(imprimerieId)
                .stream()
                .map(commandeMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Prix d'une option choisie (reliure, finition), 0 si aucune.
     *
     * Les tarifs sont indexés par l'énumération de l'option ; la commande, elle,
     * la désigne par son nom. C'est cette conversion qui manquait, et le prix
     * cherché avec une chaîne restait introuvable sans que rien ne le signale.
     *
     * @throws RuntimeException si l'option demandée n'existe pas, ou si
     *         l'imprimerie ne l'a pas tarifée — mieux vaut refuser la commande
     *         que la facturer au client sans la comptabiliser.
     */
    private <T extends Enum<T>> BigDecimal prixOption(Map<T, Double> tarifs, String optionDemandee,
                                                      Class<T> typeOption, String libelle) {
        if (optionDemandee == null || optionDemandee.isBlank() || optionDemandee.equals("AUCUNE")) {
            return BigDecimal.ZERO;
        }

        T option;
        try {
            option = Enum.valueOf(typeOption, optionDemandee);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Option de " + libelle + " inconnue : " + optionDemandee);
        }

        Double prix = tarifs != null ? tarifs.get(option) : null;
        if (prix == null) {
            throw new RuntimeException("Cette imprimerie ne propose pas la " + libelle + " « " + optionDemandee + " ».");
        }
        return BigDecimal.valueOf(prix);
    }

    /**
     * Ce vers quoi chaque statut peut évoluer.
     *
     * Deux parcours cohabitent : le retrait en magasin, où la commande passe
     * directement à « prête », et la livraison, qui passe par l'impression puis
     * l'expédition. La table réunit les deux — c'est le mode de retrait, pas
     * cette table, qui détermine lequel s'applique.
     *
     * LIVREE et ANNULEE n'ont pas de suite : une commande terminée le reste.
     */
    private static final Map<StatutCommande, Set<StatutCommande>> SUITES_POSSIBLES = Map.of(
            StatutCommande.EN_ATTENTE_PAIEMENT, EnumSet.of(StatutCommande.PAYEE,
                    StatutCommande.EN_COURS_IMPRESSION, StatutCommande.PRETE, StatutCommande.ANNULEE),
            StatutCommande.PAYEE, EnumSet.of(StatutCommande.EN_COURS_IMPRESSION,
                    StatutCommande.PRETE, StatutCommande.ANNULEE),
            StatutCommande.EN_COURS_IMPRESSION, EnumSet.of(StatutCommande.PRETE, StatutCommande.ANNULEE),
            StatutCommande.PRETE, EnumSet.of(StatutCommande.LIVREE, StatutCommande.ANNULEE),
            StatutCommande.LIVREE, EnumSet.noneOf(StatutCommande.class),
            StatutCommande.ANNULEE, EnumSet.noneOf(StatutCommande.class));

    /**
     * Met à jour le statut d'une commande (ex: PAYEE → EN_COURS_IMPRESSION → PRETE)
     */
    @Transactional
    public CommandeResponseDTO updateStatut(Long id, String nouveauStatut) {
        Commande commande = commandeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Commande non trouvée: " + id));

        StatutCommande ancien = commande.getStatut();
        StatutCommande statut = lireStatut(nouveauStatut);
        verifierTransition(ancien, statut);

        commande.setStatut(statut);
        CommandeResponseDTO dto = commandeMapper.toDto(commandeRepository.save(commande));

        if (statut != ancien) previenirSiPreteARetirer(commande, statut);
        return dto;
    }

    private StatutCommande lireStatut(String nouveauStatut) {
        try {
            return StatutCommande.valueOf(nouveauStatut);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Statut inconnu : " + nouveauStatut);
        }
    }

    /**
     * Refuse les sauts qui ne veulent rien dire.
     *
     * Rien n'empêchait jusqu'ici de ramener une commande livrée à « en attente
     * de paiement », ni de faire revivre une commande annulée. Reposer le même
     * statut reste permis : un double clic ne doit pas produire d'erreur.
     */
    private void verifierTransition(StatutCommande ancien, StatutCommande nouveau) {
        if (ancien == nouveau) return;

        if (!SUITES_POSSIBLES.getOrDefault(ancien, EnumSet.noneOf(StatutCommande.class)).contains(nouveau)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, String.format(
                    "Une commande « %s » ne peut pas passer à « %s ».", ancien, nouveau));
        }
    }

    /**
     * Prévient le client que sa commande l'attend en magasin.
     *
     * Uniquement pour un retrait : en livraison, ce même statut signifie
     * « expédiée », et annoncer au client de venir la chercher serait faux.
     * L'envoi n'a lieu qu'au changement de statut, pour qu'un imprimeur qui
     * repasse par là ne renvoie pas le message.
     */
    private void previenirSiPreteARetirer(Commande commande, StatutCommande statut) {
        if (statut != StatutCommande.PRETE || commande.getModeRetrait() != ModeRetrait.RETRAIT_MAGASIN) return;

        User client = commande.getClient();
        // Un compte supprimé porte une adresse volontairement inexistante : lui
        // écrire ne ferait qu'accumuler des échecs dans les journaux.
        if (client == null || client.estSupprime() || client.getEmail() == null) return;

        Imprimerie imprimerie = commande.getImprimerie();
        emailService.envoyerCommandePrete(
                client.getEmail(),
                client.getPrenom(),
                commande.getNumeroCommande(),
                imprimerie != null ? imprimerie.getNom() : "votre imprimerie",
                adressePostale(imprimerie),
                imprimerie != null ? imprimerie.getTelephoneContact() : null);
    }

    /** « Rue de la Loi 12, 1000 Bruxelles », en ignorant les champs absents. */
    private String adressePostale(Imprimerie imprimerie) {
        if (imprimerie == null) return "";
        return Stream.of(imprimerie.getAdresse(), imprimerie.getVille())
                .filter(champ -> champ != null && !champ.isBlank())
                .collect(Collectors.joining(", "));
    }

    /**
     * Récupère l'historique des commandes d'un client (Dashboard Client)
     */
    public List<CommandeResponseDTO> getCommandesForClient(Long clientId) {
        return commandeRepository.findParClientPourTableauDeBord(clientId)
                .stream()
                .map(commandeMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<CommandeResponseDTO> getAllCommandes() {
        return commandeRepository.findAllPourTableauDeBord()
                .stream()
                .map(commandeMapper::toDto)
                .collect(Collectors.toList());
    }
}