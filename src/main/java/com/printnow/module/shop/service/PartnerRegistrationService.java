package com.printnow.module.shop.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.printnow.module.shop.dto.HoraireOuvertureRequestDTO;
import com.printnow.module.shop.dto.ImprimerieRequestDTO;
import com.printnow.module.shop.dto.ImprimerieResponseDTO;
import com.printnow.module.shop.dto.PartnerRegistrationRequest;
import com.printnow.module.shop.dto.ProduitRequestDTO;
import com.printnow.module.shop.mapper.ShopMapper;
import com.printnow.module.shop.model.HoraireOuverture;
import com.printnow.module.shop.repository.HoraireOuvertureRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PartnerRegistrationService {

    // On injecte TES services existants !
    private final ImprimerieService imprimerieService;
    private final ProduitService produitService;
    private final HoraireOuvertureRepository horaireRepository; 
    private final ShopMapper shopMapper;
    // private final UserService userService; // (À rajouter plus tard pour créer l'utilisateur)

    @Transactional // Si un truc plante, tout s'annule (Rollback)
    public void registerNewPartner(PartnerRegistrationRequest request) {
        
        // ÉTAPE 1 : Créer le compte utilisateur (Gérant)
        // User newGerant = userService.createPartnerAccount(request.getEmail(), request.getPassword());
        Long idGerantSimule = 1L; // On simule l'ID du gérant pour l'instant

        // ÉTAPE 2 : Créer l'imprimerie en utilisant TON ImprimerieService
        ImprimerieRequestDTO shopDto = request.getImprimerie();
        shopDto.setIdGerant(idGerantSimule);
        
        // On récupère la réponse qui contient l'ID généré de l'imprimerie !
        ImprimerieResponseDTO savedShop = imprimerieService.createImprimerie(shopDto);

        // ÉTAPE 3 : Créer les produits en utilisant TON ProduitService
        if (request.getProduits() != null) {
            for (ProduitRequestDTO prodDto : request.getProduits()) {
                prodDto.setImprimerieId(savedShop.getId()); // On lie le produit à la nouvelle imprimerie
                produitService.createProduit(prodDto);
            }
        }

        // ÉTAPE 4 : Créer les horaires
        if (request.getHoraires() != null) {
            for (HoraireOuvertureRequestDTO horaireDto : request.getHoraires()) {
                horaireDto.setImprimerieId(savedShop.getId()); // On lie l'horaire à la nouvelle imprimerie
                HoraireOuverture horaire = shopMapper.toEntity(horaireDto);
                horaireRepository.save(horaire);
            }
        }
    }
}