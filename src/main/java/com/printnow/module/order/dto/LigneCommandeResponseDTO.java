package com.printnow.module.order.dto;

import java.math.BigDecimal;
import java.util.List;

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
    private String reliure;
    private String finition;
    private BigDecimal prixUnitaire;
    private BigDecimal prixTotal;
    private List<FichierPDFResponseDTO> fichiers;
}
