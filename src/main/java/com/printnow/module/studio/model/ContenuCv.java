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
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Contact(String email, String telephone, String ville) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Experience(String poste, String entreprise, String periode, String description) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Formation(String diplome, String ecole, String annee) {}
}
