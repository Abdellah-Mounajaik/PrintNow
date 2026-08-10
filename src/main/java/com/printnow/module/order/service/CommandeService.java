package com.printnow.module.order.service;

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
import com.printnow.module.shop.enums.TypeProduit;
import com.printnow.module.shop.model.Imprimerie;
import com.printnow.module.shop.model.Produit;
import com.printnow.module.shop.repository.ProduitRepository;
import com.printnow.module.user.model.User;
import lombok.RequiredArgsConstructor;
import com.printnow.module.correction.service.CorrectionCommandeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommandeService {

    private final CommandeRepository commandeRepository;
    private final ProduitRepository produitRepository;
    private final CommandeMapper commandeMapper;
    private final AdresseLivraisonMapper adresseLivraisonMapper;
    private final CodePromoService codePromoService;
    private final VerificationEtudiantRepository verificationEtudiantRepository;
    private final CorrectionCommandeService correctionCommandeService;

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

        // Ce montant est un revenu de la plateforme, jamais de l'imprimerie : on
        // le conserve à part, sans jamais l'ajouter au total de la commande ni à
        // l'assiette de la commission.
        if (montantCorrections.signum() > 0) {
            commande = enregistrerMontantCorrections(commande.getId(), montantCorrections);
        }
        return commande;
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

            // Le recto-verso n'a aucun sens pour une affiche (personne ne voit le verso) :
            // on l'ignore côté serveur quel que soit ce qu'envoie le client, pour ne pas
            // dépendre uniquement de la restriction faite côté frontend.
            boolean rectoVerso = Boolean.TRUE.equals(item.getRectoVerso())
                    && produit.getTypeProduit() != TypeProduit.POSTER;

            // Remise recto-verso (configurée par l'imprimerie, 15% par défaut) : ne
            // s'applique qu'au coût d'impression, pas à la reliure/finition ci-dessous.
            if (rectoVerso) {
                Integer pourcentageRV = produit.getImprimerie().getPourcentageRemiseRectoVerso();
                BigDecimal tauxRV = BigDecimal.valueOf(pourcentageRV != null ? pourcentageRV : 15);
                BigDecimal remiseRV = prixU.multiply(tauxRV).divide(new BigDecimal("100"));
                prixU = prixU.subtract(remiseRV).max(BigDecimal.ZERO);
            }

            // Ajout du prix de la Reliure
            if (item.getReliure() != null && !item.getReliure().equals("AUCUNE") && produit.getPrixParTypeReliure() != null) {
                Object prixRel = produit.getPrixParTypeReliure().get(item.getReliure());
                if (prixRel != null) {
                    prixU = prixU.add(new BigDecimal(prixRel.toString()));
                }
            }
            
            // Ajout du prix de la Finition / Plastification
            if (item.getFinition() != null && !item.getFinition().equals("AUCUNE") && produit.getPrixParTypePlastification() != null) {
                Object prixFin = produit.getPrixParTypePlastification().get(item.getFinition());
                if (prixFin != null) {
                    prixU = prixU.add(new BigDecimal(prixFin.toString()));
                }
            }

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
            CodePromo promo = codePromoService.appliquerCode(request.getCodePromo(), totalTTCAvantPromo, client.getId());
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
        
        // Commission (10% du TTC)
        BigDecimal commission = commande.getTotalTTC().multiply(new BigDecimal("0.10"));
        commande.setCommissionPlateforme(commission);
        
        // Montant net pour l'imprimeur
        commande.setMontantVerseImprimerie(commande.getTotalTTC().subtract(commission));

        // 8. Sauvegarde en base (sauvegarde la commande, ses lignes, l'adresse et le suivi de livraison !)
        Commande savedCommande = commandeRepository.save(commande);
        
        return commandeMapper.toDto(savedCommande);
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
        return commandeRepository.findByImprimerie_IdOrderByDateCreationDesc(imprimerieId)
                .stream()
                .map(commandeMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Met à jour le statut d'une commande (ex: PAYEE → EN_COURS_IMPRESSION → PRETE)
     */
    @Transactional
    public CommandeResponseDTO updateStatut(Long id, String nouveauStatut) {
        Commande commande = commandeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Commande non trouvée: " + id));
        commande.setStatut(StatutCommande.valueOf(nouveauStatut));
        return commandeMapper.toDto(commandeRepository.save(commande));
    }

    /**
     * Récupère l'historique des commandes d'un client (Dashboard Client)
     */
    public List<CommandeResponseDTO> getCommandesForClient(Long clientId) {
        return commandeRepository.findByClient_IdOrderByDateCreationDesc(clientId)
                .stream()
                .map(commandeMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<CommandeResponseDTO> getAllCommandes() {
        return commandeRepository.findAllByOrderByDateCreationDesc()
                .stream()
                .map(commandeMapper::toDto)
                .collect(Collectors.toList());
    }
}