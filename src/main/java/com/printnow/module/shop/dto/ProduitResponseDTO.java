package com.printnow.module.shop.dto;

import com.printnow.module.shop.enums.FormatImpression;
import com.printnow.module.shop.enums.TypeProduit;
import lombok.Data;

@Data
public class ProduitResponseDTO {
    private Long id;
    private TypeProduit typeProduit;
    private FormatImpression formatImpression;
    private Double prixBase;
    private Double prixParPage;
    private Boolean actif;
}