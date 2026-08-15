package com.printnow.module.studio.service.gabarit;

import org.apache.pdfbox.pdmodel.font.PDType1Font;

/**
 * Une combinaison palette + police, appliquée à un rendu. C'est ce qui rend deux
 * propositions visuellement différentes à partir du même contenu.
 */
public record Style(Palette palette, Police police) {
    public int[] primaire() { return palette.primaire(); }
    public int[] accent() { return palette.accent(); }
    public int[] texte() { return palette.texte(); }
    public PDType1Font normal() { return police.normal(); }
    public PDType1Font gras() { return police.gras(); }
}
