package com.printnow.module.order.dto;

import lombok.Data;

@Data
public class LigneCommandeRequestDTO {
    private Long produitId;
    private Integer quantite;
    private Integer nbPages;
    private Boolean couleur;
    private Boolean rectoVerso;
}
