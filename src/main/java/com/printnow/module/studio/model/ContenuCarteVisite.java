package com.printnow.module.studio.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Une carte de visite structurée : identité, fonction, et coordonnées. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ContenuCarteVisite(
        String nom,
        String poste,
        String entreprise,
        String telephone,
        String email,
        String siteWeb,
        String adresse
) {
    /** Consigne partagée par toutes les maquettes de carte : elles produisent le même JSON. */
    public static final String PROMPT = """
            Tu rédiges le contenu d'une carte de visite, en français, à partir de
            la description fournie.

            Réponds UNIQUEMENT par un objet JSON valide, sans texte autour, sans
            balises Markdown, exactement à ce format :
            {
              "nom": "",
              "poste": "",
              "entreprise": "",
              "telephone": "",
              "email": "",
              "siteWeb": "",
              "adresse": ""
            }

            Règles :
            - N'invente aucun fait : n'utilise que ce que la description contient.
            - "poste" est un intitulé court (ex : « Designer UX/UI »).
            - Laisse une chaîne vide quand l'information manque.
            """;
}
