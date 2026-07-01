package com.printnow.module.order.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class FichierPDFResponseDTO {
    private Long id;
    private String nomFichier;
    private Long tailleOctets;
    private Integer nbPagesDetectees;
    private LocalDateTime dateUpload;
}
