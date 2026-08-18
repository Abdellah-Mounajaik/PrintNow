package com.printnow.module.parametres.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParametresPlateformeDTO {
    private BigDecimal commissionPourcentage;
    private BigDecimal fraisInscription;
    private BigDecimal prixCorrectionForfait;
    private Integer pagesInclusesCorrection;
    private BigDecimal prixCorrectionPageSupp;
    private BigDecimal prixGenerationDesign;
}
