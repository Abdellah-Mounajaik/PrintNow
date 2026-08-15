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
) {}
