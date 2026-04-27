package com.printnow.module.shop.dto;

import lombok.Data;
import java.time.LocalTime;

@Data
public class HoraireOuvertureRequestDTO {
    private Long imprimerieId;
    private String jourSemaine;
    private LocalTime heureOuverture;
    private LocalTime heureFermeture;
    private Boolean ferme;
}