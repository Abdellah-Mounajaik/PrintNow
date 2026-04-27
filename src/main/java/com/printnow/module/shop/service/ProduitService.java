package com.printnow.module.shop.service;

import com.printnow.module.shop.dto.ProduitRequestDTO;
import com.printnow.module.shop.dto.ProduitResponseDTO;
import com.printnow.module.shop.mapper.ShopMapper;
import com.printnow.module.shop.model.Produit;
import com.printnow.module.shop.repository.ProduitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProduitService {

    private final ProduitRepository produitRepository;
    private final ShopMapper shopMapper;

    public ProduitResponseDTO createProduit(ProduitRequestDTO dto) {
        if (dto.getImprimerieId() == null) {
            throw new IllegalArgumentException("L'ID de l'imprimerie est obligatoire pour créer un produit");
        }
        
        Produit produit = shopMapper.toEntity(dto);
        // Le Mapper se charge déjà de récupérer et d'associer l'entité Imprimerie !
        return shopMapper.toResponse(produitRepository.save(produit));
    }

    @Transactional(readOnly = true)
    public List<ProduitResponseDTO> getCatalogueByImprimerie(Long imprimerieId) {
        return produitRepository.findByImprimerieIdAndActifTrue(imprimerieId).stream()
                .map(shopMapper::toResponse)
                .collect(Collectors.toList());
    }

    public void desactiverProduit(Long produitId) {
        Produit produit = produitRepository.findById(produitId)
                .orElseThrow(() -> new RuntimeException("Produit non trouvé"));
        produit.setActif(false);
        produitRepository.save(produit);
    }
}