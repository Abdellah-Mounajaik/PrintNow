package com.printnow.module.correction.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Une faute détectée dans le document, rapportée au client.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FauteDTO {

    /** Page où se trouve la faute (1 = première page). */
    private Integer page;

    /** Le mot ou groupe de mots fautif. */
    private String motFautif;

    /** Correction proposée par défaut, ou null si aucune suggestion. */
    private String correction;

    /**
     * Autres suggestions, classées. Le client peut en choisir une : les
     * correcteurs proposent parfois un mot plus fréquent que le mot voulu.
     */
    private List<String> suggestions;

    /** Explication de la règle enfreinte. */
    private String message;

    /** Extrait de la phrase, pour situer la faute. */
    private String contexte;

    /**
     * Rang de l'occurrence visée dans la page, à partir de 1.
     *
     * Un même mot peut être correct à un endroit et fautif à un autre — « nous
     * sommes sortis visiter le centre-ville », puis « nous avons visiter un
     * château ». Sans ce rang, la réécriture toucherait les deux et abîmerait la
     * phrase correcte. Vaut null quand le rang n'a pas pu être établi : toutes
     * les occurrences sont alors corrigées, comme avant.
     */
    private Integer occurrence;

    /** true si la faute a pu être corrigée directement dans le PDF. */
    private Boolean corrigeeDansPdf;
}
