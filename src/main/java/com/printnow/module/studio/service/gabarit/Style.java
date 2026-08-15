package com.printnow.module.studio.service.gabarit;

import org.apache.pdfbox.pdmodel.font.PDType1Font;

/**
 * Le style appliqué à un rendu : trois couleurs (principale pour les titres,
 * accent pour les valeurs, texte discret) et une police. C'est ce qui rend deux
 * propositions visuellement différentes à partir du même contenu.
 *
 * Les couleurs peuvent venir d'une {@link Palette} figée (secours) ou être
 * proposées par l'IA à partir du brief — d'où des composantes RVB brutes plutôt
 * qu'un enum. {@code code} sert d'étiquette stockée en base (ex : « gris-mauve »).
 */
public record Style(String code, int[] primaire, int[] accent, int[] texte, Police police) {

    /** Construit un style à partir d'une palette figée. */
    public static Style de(Palette palette, Police police) {
        return new Style(palette.code(), palette.primaire(), palette.accent(), palette.texte(), police);
    }

    public PDType1Font normal() { return police.normal(); }
    public PDType1Font gras() { return police.gras(); }
}
