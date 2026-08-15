package com.printnow.module.studio.service.gabarit;

import com.printnow.module.studio.enums.TypeSupport;

/**
 * Un type de support générable, autonome : il porte sa consigne pour l'IA (le
 * schéma JSON attendu) et sait dessiner le PDF à partir de ce JSON.
 *
 * Ajouter un type = ajouter un {@code @Component} qui implémente cette interface ;
 * {@code StudioService} le découvre et le branche tout seul, sans rien changer
 * d'autre.
 */
public interface Gabarit {

    /** Le type couvert (CV, FLYER, CARTE_VISITE…). */
    TypeSupport type();

    /** Code du gabarit, stocké sur la proposition (ex : « cv-classique »). */
    String code();

    /** Consigne système envoyée à l'IA : décrit le JSON exact à produire. */
    String promptSysteme();

    /**
     * Parse le JSON renvoyé par l'IA et dessine le PDF dans le style demandé
     * (couleurs + police). Le même JSON avec deux styles différents donne deux
     * propositions distinctes.
     *
     * @throws Exception si le JSON est absent, malformé ou inexploitable — la
     *         génération est alors marquée échouée, jamais un PDF vide rendu.
     */
    byte[] rendre(String json, Style style) throws Exception;
}
