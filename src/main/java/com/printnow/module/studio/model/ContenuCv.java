package com.printnow.module.studio.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Le CV tel que l'IA le renvoie, structuré. C'est la « source de vérité » du
 * rendu : le gabarit se contente de poser ces champs à des positions fixes, si
 * bien que le même contenu produit toujours le même PDF.
 *
 * Tolérant aux champs inconnus : un modèle de langue ajoute parfois des clés.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ContenuCv(
        String nom,
        String titrePro,
        Contact contact,
        List<String> competences,
        List<Experience> experiences,
        List<Formation> formations,
        List<String> langues
) {
    /** Consigne partagée par toutes les maquettes de CV : elles produisent le même JSON. */
    public static final String PROMPT = """
            Tu es un assistant qui met en forme des CV. À partir de la description
            fournie par l'utilisateur, produis un CV structuré, en français.

            Réponds UNIQUEMENT par un objet JSON valide, sans texte autour, sans
            balises Markdown, exactement à ce format :
            {
              "nom": "",
              "titrePro": "",
              "contact": { "email": "", "telephone": "", "ville": "" },
              "competences": [],
              "experiences": [ { "poste": "", "entreprise": "", "periode": "", "description": "" } ],
              "formations": [ { "diplome": "", "ecole": "", "annee": "" } ],
              "langues": []
            }

            Règles :
            - N'invente aucun fait : n'utilise que ce que la description contient.
            - Améliore la formulation (style professionnel), sans mentir.
            - Laisse une chaîne vide ou une liste vide quand l'information manque.
            - "titrePro" est un intitulé court (ex : « Développeur Full-Stack »).
            """;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Contact(String email, String telephone, String ville) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Experience(String poste, String entreprise, String periode, String description) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Formation(String diplome, String ecole, String annee) {}
}
