package com.printnow.module.shop.mapper;

import com.printnow.module.shop.dto.*;
import com.printnow.module.shop.model.*;
import com.printnow.module.shop.repository.ImprimerieRepository;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
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

    /**
     * Une modification ne touche que les champs effectivement transmis.
     *
     * Sans IGNORE, MapStruct remettait à null tout ce que la requête ne
     * mentionnait pas : une mise à jour portant sur le seul nom effaçait
     * l'adresse, le logo, les tarifs et les options de l'imprimerie. Les deux
     * autres mises à jour de ce mapper s'en protégeaient déjà ; celle-ci ne le
     * faisait pas.
     *
     * Les coordonnées GPS restent exclues à part : calculées par le serveur
     * depuis l'adresse, elles ne figurent jamais dans ce que le navigateur
     * renvoie.
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "latitude", ignore = true)
    @Mapping(target = "longitude", ignore = true)
    public abstract void updateImprimerieFromDto(ImprimerieRequestDTO dto, @MappingTarget Imprimerie entity);


    // ==========================================
    // MAPPINGS PRODUIT
    // ==========================================

    public abstract ProduitResponseDTO toResponse(Produit entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "actif", constant = "true")
    @Mapping(target = "imprimerie", expression = "java(dto.getImprimerieId() != null ? imprimerieRepository.findById(dto.getImprimerieId()).orElse(null) : null)")
    public abstract Produit toEntity(ProduitRequestDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "imprimerie", ignore = true)
    public abstract void updateProduitFromDto(ProduitRequestDTO dto, @MappingTarget Produit entity);


    // ==========================================
    // MAPPINGS HORAIRES
    // ==========================================

    public abstract HoraireOuvertureResponseDTO toResponse(HoraireOuverture entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "imprimerie", expression = "java(dto.getImprimerieId() != null ? imprimerieRepository.findById(dto.getImprimerieId()).orElse(null) : null)")
    public abstract HoraireOuverture toEntity(HoraireOuvertureRequestDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "imprimerie", ignore = true)
    public abstract void updateHoraireFromDto(HoraireOuvertureRequestDTO dto, @MappingTarget HoraireOuverture entity);
}