package com.printnow.module.shop.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

import com.printnow.module.user.mapper.UserMapper;
import com.printnow.module.user.model.Role;
import com.printnow.module.user.model.User;
import com.printnow.module.user.repository.RoleRepository;
import com.printnow.module.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PartnerRegistrationService {

    private final ImprimerieService imprimerieService;
    private final ProduitService produitService;
    private final ImprimerieRepository imprimerieRepository;
    private final HoraireOuvertureRepository horaireRepository; 
    private final ShopMapper shopMapper;
    
    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Crée un nouveau partenaire (User + Imprimerie + Produits + Horaires)
     * en mode inactif en attendant le paiement Stripe.
     */
    @Transactional
    public Long registerNewPartner(PartnerRegistrationRequest request) {
        // Validation de base
        if (request.getSiret() == null || request.getSiret().isBlank()) {
            throw new IllegalArgumentException("Le numéro de TVA est obligatoire.");
        }
        
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Cette adresse email est déjà utilisée.");
        }

        // 1. RÉCUPÉRER LE RÔLE
        Role roleImprimerie = roleRepository.findByNom("IMPRIMERIE")
                .orElseThrow(() -> new RuntimeException("Erreur : Rôle IMPRIMERIE non trouvé."));

        // 2. CRÉER L'UTILISATEUR (Gérant)
        User newGerant = userMapper.toEntityFromPartnerRequest(request);
        newGerant.setMotDePasse(passwordEncoder.encode(request.getPassword()));
        newGerant.setRole(roleImprimerie);
        newGerant.setActif(true); 
        
        newGerant = userRepository.save(newGerant);

        // 3. CRÉER L'IMPRIMERIE
        ImprimerieRequestDTO shopDto = request.getImprimerie();
        shopDto.setIdGerant(newGerant.getId());
        shopDto.setNumeroTva(request.getSiret());

        ImprimerieResponseDTO savedShop = imprimerieService.createImprimerie(shopDto);

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

        // Retourne l'ID pour le contrôleur et Stripe
        return savedShop.getId();
    }

    /**
     * Active le compte suite à la confirmation du Webhook Stripe.
     */
    @Transactional
    public void activatePartnerAccount(Long imprimerieId) {
        Imprimerie imprimerie = imprimerieRepository.findById(imprimerieId)
            .orElseThrow(() -> new RuntimeException("Imprimerie non trouvée"));
        
        // 1. Activer l'imprimerie
        imprimerie.setActif(true);
        imprimerieRepository.save(imprimerie); // Sauvegarde l'imprimerie
        
        // 2. Activer le gérant explicitement
        if (imprimerie.getGerant() != null) {
            User gerant = imprimerie.getGerant();
            gerant.setActif(true);
            userRepository.save(gerant); // 💡 TRÈS IMPORTANT : Sauvegarde l'utilisateur explicitement
            System.out.println("DEBUG: L'utilisateur " + gerant.getEmail() + " a été activé.");
        }
    }
}