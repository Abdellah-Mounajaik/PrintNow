package com.printnow.module.shop.dto;

import com.printnow.module.shop.enums.FormatImpression;
import com.printnow.module.shop.enums.TypeProduit;
import com.printnow.module.shop.enums.TypePlastification;
import com.printnow.module.shop.enums.TypeReliure;
import lombok.Data;

import java.util.List;

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
    private List<TypePlastification> typesPlastification;

    // ==========================================
    // NOUVEAUX CHAMPS RELIURE
    // ==========================================
    private Boolean proposeReliure;
    private Double prixReliure;
    private List<TypeReliure> typesReliure;
}