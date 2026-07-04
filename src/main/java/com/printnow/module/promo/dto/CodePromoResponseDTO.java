package com.printnow.module.promo.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CodePromoResponseDTO {
    private Long id;
    private String code;
    private String typeReduction;
    private BigDecimal valeurReduction;
    private BigDecimal montantMinimumCommande;
    private java.time.LocalDateTime dateDebut;
    private java.time.LocalDateTime dateFin;
    private Integer utilisationMax;
    private Integer utilisationCourante;
    private Boolean actif;
}
