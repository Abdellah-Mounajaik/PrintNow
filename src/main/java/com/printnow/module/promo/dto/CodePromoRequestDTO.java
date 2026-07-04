package com.printnow.module.promo.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CodePromoRequestDTO {
    private String code;
    private String typeReduction;
    private BigDecimal valeurReduction;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private Integer utilisationMax;
    private BigDecimal montantMinimumCommande;
    private Long imprimerieId;
}
