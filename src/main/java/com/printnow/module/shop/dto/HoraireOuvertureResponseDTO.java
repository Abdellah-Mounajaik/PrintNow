package com.printnow.module.shop.dto;

import lombok.Data;
import java.time.LocalTime;

@Data
public class HoraireOuvertureResponseDTO {
    private Long id;
    private String jourSemaine;
    private LocalTime heureOuverture;
    private LocalTime heureFermeture;
    private Boolean ferme;
}
