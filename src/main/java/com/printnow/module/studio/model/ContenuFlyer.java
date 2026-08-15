package com.printnow.module.studio.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Un flyer tel que l'IA le renvoie, structuré : un titre accrocheur, une
 * accroche, quelques blocs d'information, et un contact.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ContenuFlyer(
        String titre,
        String accroche,
        List<Bloc> blocs,
        Contact contact
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Bloc(String libelle, String valeur) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Contact(String telephone, String adresse, String email) {}
}
