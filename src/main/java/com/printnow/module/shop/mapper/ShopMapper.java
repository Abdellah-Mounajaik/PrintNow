package com.printnow.module.shop.mapper;

import com.printnow.module.shop.dto.*;
import com.printnow.module.shop.model.*;
import com.printnow.module.shop.repository.ImprimerieRepository;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class ShopMapper {

    @Autowired
    protected ImprimerieRepository imprimerieRepository;

    // ==========================================
    // MAPPINGS IMPRIMERIE
    // ==========================================
    
    public abstract ImprimerieResponseDTO toResponse(Imprimerie entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "actif", constant = "true") // Par défaut, une nouvelle imprimerie est active
    public abstract Imprimerie toEntity(ImprimerieRequestDTO dto);

    @Mapping(target = "id", ignore = true)
    public abstract void updateImprimerieFromDto(ImprimerieRequestDTO dto, @MappingTarget Imprimerie entity);


    // ==========================================
    // MAPPINGS PRODUIT
    // ==========================================

    public abstract ProduitResponseDTO toResponse(Produit entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "actif", constant = "true")
    // Recherche de l'imprimerie en base de données grâce à l'ID fourni dans le DTO
    @Mapping(target = "imprimerie", expression = "java(dto.getImprimerieId() != null ? imprimerieRepository.findById(dto.getImprimerieId()).orElse(null) : null)")
    public abstract Produit toEntity(ProduitRequestDTO dto);


    // ==========================================
    // MAPPINGS HORAIRES
    // ==========================================

    public abstract HoraireOuvertureResponseDTO toResponse(HoraireOuverture entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "imprimerie", expression = "java(dto.getImprimerieId() != null ? imprimerieRepository.findById(dto.getImprimerieId()).orElse(null) : null)")
    public abstract HoraireOuverture toEntity(HoraireOuvertureRequestDTO dto);
}