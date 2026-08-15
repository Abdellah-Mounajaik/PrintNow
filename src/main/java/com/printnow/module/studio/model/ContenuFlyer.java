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
    /** Consigne partagée par toutes les maquettes de flyer : elles produisent le même JSON. */
    public static final String PROMPT = """
            Tu rédiges le contenu d'un flyer promotionnel, en français, à partir de
            la description fournie.

            Réponds UNIQUEMENT par un objet JSON valide, sans texte autour, sans
            balises Markdown, exactement à ce format :
            {
              "titre": "",
              "accroche": "",
              "blocs": [ { "libelle": "", "valeur": "" } ],
              "contact": { "telephone": "", "adresse": "", "email": "" }
            }

            Règles :
            - N'invente aucun fait : n'utilise que ce que la description contient.
            - "titre" est court et accrocheur ; "accroche" est une phrase percutante.
            - "blocs" liste les informations clés (ex : « Menu du midi » / « 15 € »).
            - Laisse une chaîne vide quand l'information manque.
            """;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Bloc(String libelle, String valeur) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Contact(String telephone, String adresse, String email) {}
}
