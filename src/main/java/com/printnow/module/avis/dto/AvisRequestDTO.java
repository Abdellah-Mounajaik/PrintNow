package com.printnow.module.avis.dto;

import lombok.Data;

@Data
public class AvisRequestDTO {
    private Long imprimerieId;
    private Integer note;        // 1 à 5
    private String commentaire;
}
