package com.printnow.module.order.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LivraisonSuiviDTO {
    private Long commandeId;
    private String numeroSuivi;
    private String statut;           // Notre enum interne (EN_PREPARATION, EXPEDIE, EN_COURS, LIVRE, ECHEC)
    private String statutAfterShipping; // Tag brut AfterShipping (InTransit, Delivered…)
    private String lienSuiviBpost;   // Lien direct vers le suivi bpost
}
