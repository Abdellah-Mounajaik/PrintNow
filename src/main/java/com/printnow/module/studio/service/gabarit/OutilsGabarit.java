package com.printnow.module.studio.service.gabarit;

import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Petits outils de mise en page réutilisés par les maquettes : découpe de texte,
 * ajustement de taille, éclaircissement/assombrissement de couleur pour garder
 * un bon contraste. Sans état, tout en méthodes statiques.
 */
final class OutilsGabarit {

    private OutilsGabarit() {
    }

    static boolean rempli(String s) {
        return s != null && !s.isBlank();
    }

    static boolean vide(List<?> liste) {
        return liste == null || liste.isEmpty();
    }

    static String joindre(String sep, String a, String b) {
        boolean ra = rempli(a), rb = rempli(b);
        if (ra && rb) return a + sep + b;
        return ra ? a : (rb ? b : "");
    }

    /** Coupe un texte en lignes qui tiennent dans la largeur donnée. */
    static List<String> envelopper(PDType1Font font, float taille, String texte, float largeurMax) throws IOException {
        List<String> lignes = new ArrayList<>();
        if (!rempli(texte)) return lignes;
        StringBuilder courante = new StringBuilder();
        for (String mot : texte.split("\\s+")) {
            String essai = courante.isEmpty() ? mot : courante + " " + mot;
            if (font.getStringWidth(essai) / 1000f * taille > largeurMax && courante.length() > 0) {
                lignes.add(courante.toString());
                courante = new StringBuilder(mot);
            } else {
                courante = new StringBuilder(essai);
            }
        }
        if (courante.length() > 0) lignes.add(courante.toString());
        return lignes;
    }

    /** Plus grande taille (entre min et préférée) à laquelle le texte tient dans la largeur. */
    static float tailleQuiTient(PDType1Font font, String texte, float taillePref, float tailleMin, float largeurMax) throws IOException {
        float taille = taillePref;
        while (taille > tailleMin && font.getStringWidth(texte) / 1000f * taille > largeurMax) {
            taille -= 0.5f;
        }
        return taille;
    }

    static String motLePlusLong(String texte) {
        String max = "";
        for (String mot : texte.split("\\s+")) {
            if (mot.length() > max.length()) max = mot;
        }
        return max.isBlank() ? texte : max;
    }

    /** Assombrit une couleur jusqu'à une luminance cible, pour garantir du texte blanc lisible dessus. */
    static int[] foncer(int[] rgb, double lumMax) {
        double r = rgb[0], v = rgb[1], b = rgb[2];
        double lum = luminance(r, v, b);
        if (lum > lumMax && lum > 0) {
            double f = lumMax / lum;
            r *= f; v *= f; b *= f;
        }
        return new int[]{arrondi(r), arrondi(v), arrondi(b)};
    }

    /** Éclaircit une couleur jusqu'à une luminance cible, pour qu'elle ressorte sur un aplat sombre. */
    static int[] eclaircir(int[] rgb, double lumMin) {
        double r = rgb[0], v = rgb[1], b = rgb[2];
        double lum = luminance(r, v, b);
        if (lum <= 0) return new int[]{(int) lumMin, (int) lumMin, (int) lumMin};
        if (lum < lumMin) {
            double f = lumMin / lum;
            r = Math.min(255, r * f); v = Math.min(255, v * f); b = Math.min(255, b * f);
        }
        return new int[]{arrondi(r), arrondi(v), arrondi(b)};
    }

    /** Mélange une couleur vers le blanc (fraction 0..1) : sert à éclaircir un aplat déjà sombre. */
    static int[] melerBlanc(int[] rgb, double fraction) {
        return new int[]{
                arrondi(rgb[0] + (255 - rgb[0]) * fraction),
                arrondi(rgb[1] + (255 - rgb[1]) * fraction),
                arrondi(rgb[2] + (255 - rgb[2]) * fraction)
        };
    }

    private static double luminance(double r, double v, double b) {
        return 0.2126 * r + 0.7152 * v + 0.0722 * b;
    }

    private static int arrondi(double x) {
        return (int) Math.round(x);
    }
}
