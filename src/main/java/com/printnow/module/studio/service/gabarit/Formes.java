package com.printnow.module.studio.service.gabarit;

import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.IOException;

/**
 * Primitives de dessin « modernes » directement sur le flux de contenu :
 * disques, anneaux, polygones (découpes en diagonale), rectangles arrondis et
 * texte en lettres espacées. De quoi sortir du tout-rectangle sans image.
 */
final class Formes {

    /** Constante de Bézier pour approximer un quart de cercle. */
    private static final float KAPPA = 0.5522847498f;

    private Formes() {
    }

    /** Disque plein centré en (cx, cy). */
    static void disque(PDPageContentStream cs, float cx, float cy, float r, int[] rgb) throws IOException {
        remplissage(cs, rgb);
        cercle(cs, cx, cy, r);
        cs.fill();
        reinit(cs);
    }

    /** Anneau (cercle non rempli) d'épaisseur donnée. */
    static void anneau(PDPageContentStream cs, float cx, float cy, float r, float epaisseur, int[] rgb) throws IOException {
        cs.setStrokingColor(rgb[0] / 255f, rgb[1] / 255f, rgb[2] / 255f);
        cs.setLineWidth(epaisseur);
        cercle(cs, cx, cy, r);
        cs.stroke();
        cs.setStrokingColor(0f, 0f, 0f);
        cs.setLineWidth(1f);
    }

    /** Polygone plein (par ex. une découpe en diagonale). */
    static void polygone(PDPageContentStream cs, float[] xs, float[] ys, int[] rgb) throws IOException {
        remplissage(cs, rgb);
        cs.moveTo(xs[0], ys[0]);
        for (int i = 1; i < xs.length; i++) {
            cs.lineTo(xs[i], ys[i]);
        }
        cs.closePath();
        cs.fill();
        reinit(cs);
    }

    /** Rectangle plein aux coins arrondis. */
    static void rectArrondi(PDPageContentStream cs, float x, float y, float w, float h, float r, int[] rgb) throws IOException {
        float k = KAPPA * r;
        remplissage(cs, rgb);
        cs.moveTo(x + r, y);
        cs.lineTo(x + w - r, y);
        cs.curveTo(x + w - r + k, y, x + w, y + r - k, x + w, y + r);
        cs.lineTo(x + w, y + h - r);
        cs.curveTo(x + w, y + h - r + k, x + w - r + k, y + h, x + w - r, y + h);
        cs.lineTo(x + r, y + h);
        cs.curveTo(x + r - k, y + h, x, y + h - r + k, x, y + h - r);
        cs.lineTo(x, y + r);
        cs.curveTo(x, y + r - k, x + r - k, y, x + r, y);
        cs.fill();
        reinit(cs);
    }

    /** Texte en lettres espacées (tracking), utile pour les micro-labels en majuscules. */
    static void texteEspace(PDPageContentStream cs, PDType1Font font, float taille, float x, float y,
                            String texte, float espacement, int[] rgb) throws IOException {
        cs.beginText();
        cs.setFont(font, taille);
        cs.setCharacterSpacing(espacement);
        cs.setNonStrokingColor(rgb[0] / 255f, rgb[1] / 255f, rgb[2] / 255f);
        cs.newLineAtOffset(x, y);
        cs.showText(texte);
        cs.endText();
        cs.setCharacterSpacing(0);
        cs.setNonStrokingColor(0f, 0f, 0f);
    }

    /** Largeur d'un texte en lettres espacées, pour le centrer. */
    static float largeurEspace(PDType1Font font, float taille, String texte, float espacement) throws IOException {
        float base = font.getStringWidth(texte) / 1000f * taille;
        return base + Math.max(0, texte.length() - 1) * espacement;
    }

    /** Marqueur de liste selon la variante (point, carré ou tiret), centré en (x, y). */
    static void marqueur(PDPageContentStream cs, Variante.Puce puce, float x, float y, float taille, int[] rgb) throws IOException {
        switch (puce) {
            case POINT -> disque(cs, x, y, taille, rgb);
            case CARRE -> {
                remplissage(cs, rgb);
                cs.addRect(x - taille, y - taille, taille * 2, taille * 2);
                cs.fill();
                reinit(cs);
            }
            case TIRET -> {
                remplissage(cs, rgb);
                cs.addRect(x - taille * 1.7f, y - taille * 0.45f, taille * 3.4f, taille * 0.9f);
                cs.fill();
                reinit(cs);
            }
        }
    }

    /** Pastille de monogramme selon la variante ; renvoie true si elle est pleine (initiales à contraster). */
    static boolean badge(PDPageContentStream cs, Variante.Badge badge, float cx, float cy, float r, int[] rgb) throws IOException {
        switch (badge) {
            case ANNEAU -> {
                anneau(cs, cx, cy, r, 2.4f, rgb);
                return false;
            }
            case DISQUE -> {
                disque(cs, cx, cy, r, rgb);
                return true;
            }
            case CARRE -> {
                rectArrondi(cs, cx - r, cy - r, 2 * r, 2 * r, r * 0.4f, rgb);
                return true;
            }
        }
        return false;
    }

    /** Trait d'accent sous un titre selon la variante ; (x, y) = coin bas-gauche. */
    static void souligne(PDPageContentStream cs, Variante.Trait trait, float x, float y, int[] rgb) throws IOException {
        switch (trait) {
            case COURT -> {
                remplissage(cs, rgb);
                cs.addRect(x, y, 30, 2.5f);
                cs.fill();
                reinit(cs);
            }
            case LONG -> {
                remplissage(cs, rgb);
                cs.addRect(x, y, 62, 2.5f);
                cs.fill();
                reinit(cs);
            }
            case POINTS -> {
                for (int i = 0; i < 4; i++) disque(cs, x + 3 + i * 9, y + 1.4f, 1.8f, rgb);
            }
        }
    }

    /** Initiales (1 à 2 lettres) tirées d'un nom, pour un monogramme. */
    static String initiales(String nom) {
        if (nom == null) return "";
        StringBuilder sb = new StringBuilder();
        for (String mot : nom.trim().split("\\s+")) {
            if (!mot.isBlank()) {
                sb.append(Character.toUpperCase(mot.charAt(0)));
            }
            if (sb.length() >= 2) break;
        }
        return sb.toString();
    }

    private static void cercle(PDPageContentStream cs, float cx, float cy, float r) throws IOException {
        float k = KAPPA * r;
        cs.moveTo(cx - r, cy);
        cs.curveTo(cx - r, cy + k, cx - k, cy + r, cx, cy + r);
        cs.curveTo(cx + k, cy + r, cx + r, cy + k, cx + r, cy);
        cs.curveTo(cx + r, cy - k, cx + k, cy - r, cx, cy - r);
        cs.curveTo(cx - k, cy - r, cx - r, cy - k, cx - r, cy);
    }

    private static void remplissage(PDPageContentStream cs, int[] rgb) throws IOException {
        cs.setNonStrokingColor(rgb[0] / 255f, rgb[1] / 255f, rgb[2] / 255f);
    }

    private static void reinit(PDPageContentStream cs) throws IOException {
        cs.setNonStrokingColor(0f, 0f, 0f);
    }
}
