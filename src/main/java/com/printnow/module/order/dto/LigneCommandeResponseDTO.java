package com.printnow.module.order.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class LigneCommandeResponseDTO {
private Long id;
    private Long produitId;
    private String nomProduit;
    private Integer quantite;
    private Integer nbPages;
    private Boolean couleur;
    private Boolean rectoVerso;
    private BigDecimal prixUnitaire;
    private BigDecimal prixTotal;
}
