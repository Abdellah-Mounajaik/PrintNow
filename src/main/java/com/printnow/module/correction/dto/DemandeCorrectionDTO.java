package com.printnow.module.correction.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Correction demandée par le client au moment de valider sa commande.
 */
@Data
public class DemandeCorrectionDTO {

    /** Identifiant de l'analyse déjà réalisée gratuitement. */
    private Long verificationId;

    /**
     * Positions, dans la liste des fautes, que le client a écartées : noms
     * propres, termes métier, choix volontaires.
     */
    private List<Integer> fautesIgnorees;

    /**
     * Corrections choisies par le client lorsqu'il préfère une autre suggestion
     * que celle proposée par défaut : position de la faute → mot retenu.
     */
    private Map<Integer, String> remplacementsChoisis;
}
