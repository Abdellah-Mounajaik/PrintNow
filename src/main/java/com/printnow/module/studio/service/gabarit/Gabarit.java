package com.printnow.module.studio.service.gabarit;

import com.printnow.module.studio.enums.TypeSupport;

import java.util.List;

/**
 * Un type de support générable (CV, flyer, carte). Il porte la consigne IA (le
 * schéma JSON attendu) et sait rendre son PDF dans l'une de ses structures.
 *
 * Les structures sont de simples identifiants (« moderne », « minimal »…) : le
 * rendu est du HTML/CSS, donc en ajouter une = ajouter un identifiant ici et un
 * bloc CSS dans le builder correspondant. {@code StudioService} en tire 3 au
 * hasard par génération.
 */
public interface Gabarit {

    /** Le type couvert (CV, FLYER, CARTE_VISITE). */
    TypeSupport type();

    /** Consigne système envoyée à l'IA : décrit le JSON exact à produire. */
    String promptSysteme();

    /** Les structures disponibles pour ce type (le pool où l'on pioche). */
    List<String> structures();

    /**
     * Parse le JSON de l'IA et rend le PDF dans la structure et le style demandés.
     *
     * @throws Exception si le JSON est absent, malformé ou inexploitable.
     */
    byte[] rendre(String json, Style style, String structure) throws Exception;
}
