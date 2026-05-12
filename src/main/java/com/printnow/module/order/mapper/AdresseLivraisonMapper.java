package com.printnow.module.order.mapper;


import org.springframework.stereotype.Component;

import com.printnow.module.order.dto.AdresseLivraisonRequestDTO;
import com.printnow.module.order.dto.AdresseLivraisonResponseDTO;
import com.printnow.module.order.model.AdresseLivraison;

@Component
public class AdresseLivraisonMapper {

    // Frontend -> Backend (Sauvegarde en BDD)
    public AdresseLivraison toEntity(AdresseLivraisonRequestDTO dto) {
        if (dto == null) {
            return null;
        }

        return AdresseLivraison.builder()
                .nomDestinataire(dto.getNomDestinataire())
                .rue(dto.getRue())
                .numero(dto.getNumero())
                .codePostal(dto.getCodePostal())
                .ville(dto.getVille())
                .pays(dto.getPays())
                .telephone(dto.getTelephone())
                .build();
    }

    // Backend -> Frontend (Affichage)
    public AdresseLivraisonResponseDTO toDto(AdresseLivraison entity) {
        if (entity == null) {
            return null;
        }

        return AdresseLivraisonResponseDTO.builder()
                .id(entity.getId())
                .nomDestinataire(entity.getNomDestinataire())
                .rue(entity.getRue())
                .numero(entity.getNumero())
                .codePostal(entity.getCodePostal())
                .ville(entity.getVille())
                .pays(entity.getPays())
                .telephone(entity.getTelephone())
                .build();
    }
}