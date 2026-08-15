package com.printnow.module.studio.service.gabarit;

import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

/**
 * Une famille de police, en deux graisses. On se limite aux polices standard de
 * PDFBox (incorporées d'office, aucun fichier à embarquer) : une sans-serif
 * moderne et une serif classique.
 */
public enum Police {
    MODERNE("moderne", Standard14Fonts.FontName.HELVETICA, Standard14Fonts.FontName.HELVETICA_BOLD),
    CLASSIQUE("classique", Standard14Fonts.FontName.TIMES_ROMAN, Standard14Fonts.FontName.TIMES_BOLD);

    private final String code;
    private final PDType1Font normal;
    private final PDType1Font gras;

    Police(String code, Standard14Fonts.FontName normal, Standard14Fonts.FontName gras) {
        this.code = code;
        this.normal = new PDType1Font(normal);
        this.gras = new PDType1Font(gras);
    }

    public String code() { return code; }
    public PDType1Font normal() { return normal; }
    public PDType1Font gras() { return gras; }
}
