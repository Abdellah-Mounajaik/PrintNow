package com.printnow.module.correction.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Réponse renvoyée au client. Tant que le paiement n'est pas confirmé, la liste
 * des fautes reste vide : seul le décompte est communiqué.
 */
@Data
@AllArgsConstructor
public class VerificationResponseDTO {
    private Long id;
    private String nomFichier;
    private Integer nbPages;
    private Integer nbFautes;
    /** Langue dans laquelle le document a été relu (« français », « anglais »…). */
    private String langue;
    private BigDecimal prix;
    private Boolean payee;
    /** Nombre de fautes réécrites dans le PDF (null tant que non payé). */
    private Integer nbCorrigees;
    /** Détail des fautes, uniquement après paiement. */
    private List<FauteDTO> fautes;
}
