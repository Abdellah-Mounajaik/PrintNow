package com.printnow.module.shop.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.printnow.infrastructure.email.EmailService;
import com.printnow.module.shop.dto.HoraireOuvertureRequestDTO;
import com.printnow.module.shop.dto.ImprimerieRequestDTO;
import com.printnow.module.shop.dto.ImprimerieResponseDTO;
import com.printnow.module.shop.dto.PartnerRegistrationRequest;
import com.printnow.module.shop.dto.ProduitRequestDTO;
import com.printnow.module.shop.mapper.ShopMapper;
import com.printnow.module.shop.model.HoraireOuverture;
import com.printnow.module.shop.model.Imprimerie;
import com.printnow.module.shop.repository.HoraireOuvertureRepository;
import com.printnow.module.shop.repository.ImprimerieRepository;
import com.printnow.module.parametres.service.ParametresService;
import com.printnow.module.order.service.FactureInscriptionService;

import com.printnow.module.user.mapper.UserMapper;
import com.printnow.module.user.model.Role;
import com.printnow.module.user.model.User;
import com.printnow.module.user.repository.RoleRepository;
import com.printnow.module.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PartnerRegistrationService {

    private final ImprimerieService imprimerieService;
    private final ImprimerieRepository imprimerieRepository;
    private final ProduitService produitService;
    private final HoraireOuvertureRepository horaireRepository;
    private final ShopMapper shopMapper;

    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final ParametresService parametresService;
    private final FactureInscriptionService factureInscriptionService;

    /**
     * Crée un nouveau partenaire (User + Imprimerie + Produits + Horaires), déjà actif.
     * N'est appelé qu'après vérification par le contrôleur que le paiement Stripe
     * a bien été confirmé : aucune ligne n'est créée en base en cas de paiement échoué.
     */
    @Transactional
    public Long registerNewPartner(PartnerRegistrationRequest request) {
        // Validation de base
        if (request.getSiret() == null || request.getSiret().isBlank()) {
            throw new IllegalArgumentException("Le numéro de TVA est obligatoire.");
        }

        // Revérifié ici même si l'étape 1 du formulaire l'a déjà fait : entre les
        // deux, quelqu'un d'autre a pu prendre le même email ou numéro de TVA.
        verifierDisponibilite(request.getEmail(), request.getSiret());

        // 1. RÉCUPÉRER LE RÔLE
        Role roleImprimerie = roleRepository.findByNom("IMPRIMERIE")
                .orElseThrow(() -> new RuntimeException("Erreur : Rôle IMPRIMERIE non trouvé."));

        // 2. CRÉER L'UTILISATEUR (Gérant) — le paiement est déjà confirmé à ce stade
        User newGerant = userMapper.toEntityFromPartnerRequest(request);
        newGerant.setMotDePasse(passwordEncoder.encode(request.getPassword()));
        newGerant.setRole(roleImprimerie);
        newGerant.setActif(true);

        newGerant = userRepository.save(newGerant);

        // 3. CRÉER L'IMPRIMERIE (active par défaut via le mapper)
        ImprimerieRequestDTO shopDto = request.getImprimerie();
        shopDto.setIdGerant(newGerant.getId());
        shopDto.setNumeroTva(request.getSiret());

        ImprimerieResponseDTO savedShop = imprimerieService.createImprimerie(shopDto);

        // 3bis. Frais d'inscription : figés ici une fois pour toutes (numéro, date,
        // montant réellement payé), pour que la facture reste exacte même si le
        // tarif configuré change ensuite.
        Imprimerie imprimerie = imprimerieRepository.findById(savedShop.getId())
                .orElseThrow(() -> new RuntimeException("Erreur : imprimerie introuvable après création."));
        imprimerie.setNumeroFactureInscription(genererNumeroFactureInscription());
        imprimerie.setDateInscription(LocalDateTime.now());
        imprimerie.setMontantFraisInscription(parametresService.getParametres().getFraisInscription());
        imprimerie.setPaymentIntentId(request.getPaymentIntentId());
        imprimerie = imprimerieRepository.save(imprimerie);

        // 4. CRÉER LES PRODUITS
        if (request.getProduits() != null) {
            for (ProduitRequestDTO prodDto : request.getProduits()) {
                prodDto.setImprimerieId(savedShop.getId());
                produitService.createProduit(prodDto);
            }
        }

        // 5. CRÉER LES HORAIRES
        if (request.getHoraires() != null) {
            for (HoraireOuvertureRequestDTO horaireDto : request.getHoraires()) {
                horaireDto.setImprimerieId(savedShop.getId());
                HoraireOuverture horaire = shopMapper.toEntity(horaireDto);
                horaireRepository.save(horaire);
            }
        }

        // 6. Mail de bienvenue partenaire, avec la facture des frais d'inscription en
        // pièce jointe (asynchrone, ne doit jamais faire échouer l'inscription).
        // Un échec de génération PDF ne doit pas non plus priver le gérant de son
        // mail de bienvenue : la facture reste de toute façon téléchargeable depuis
        // son espace professionnel.
        byte[] facturePdf = null;
        try {
            facturePdf = factureInscriptionService.genererPourEnvoiEmail(imprimerie);
        } catch (Exception e) {
            // volontairement silencieux ici : voir le commentaire ci-dessus.
        }
        emailService.envoyerBienvenuePartenaire(newGerant.getEmail(), savedShop.getNom(), facturePdf);

        return savedShop.getId();
    }

    /**
     * Vérifie que l'email et le numéro de TVA sont encore libres.
     *
     * Appelée avant le paiement (étape 1 du formulaire) pour prévenir plutôt que
     * guérir, et revérifiée à l'inscription elle-même : sans ce deuxième appel,
     * la carte du client serait débitée avant qu'on découvre le conflit — trop
     * tard pour le prévenir sans le rembourser.
     *
     * @throws ResponseStatusException 409 si l'un des deux est déjà utilisé
     */
    public void verifierDisponibilite(String email, String numeroTva) {
        if (email != null && !email.isBlank() && userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette adresse email est déjà utilisée.");
        }
        if (numeroTva != null && !numeroTva.isBlank() && imprimerieRepository.existsByNumeroTva(numeroTva)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce numéro de TVA est déjà utilisé par une autre imprimerie.");
        }
    }

    private String genererNumeroFactureInscription() {
        return "INSC-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
    }
}