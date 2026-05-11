package com.printnow.module.shop.dto;

import com.printnow.module.shop.enums.FormatImpression;
import com.printnow.module.shop.enums.TypeProduit;
import com.printnow.module.shop.enums.TypePlastification;
import com.printnow.module.shop.enums.TypeReliure;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ProduitResponseDTO {
    private Long id;
    private TypeProduit typeProduit;
    private FormatImpression formatImpression;
    private Double prixBase;
    private Double prixParPage;
    private Boolean actif;

    // ==========================================
    // NOUVEAUX CHAMPS PLASTIFICATION
    // ==========================================
    private Boolean proposePlastification;
    private Double prixPlastification;
    private Map<TypePlastification, Double> prixParTypePlastification;

    // ==========================================
    // NOUVEAUX CHAMPS RELIURE
    // ==========================================
    private Boolean proposeReliure;
    private Double prixReliure;
    private Map<TypeReliure, Double> prixParTypeReliure;
}