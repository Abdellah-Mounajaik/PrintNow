package com.printnow.module.studio.service.gabarit;

/**
 * Calculs de couleur partagés par le {@link Style} : luminance, éclaircissement /
 * assombrissement vers une cible, et contraste garanti d'une couleur sur une
 * surface (claire ou sombre). Sans état, tout en méthodes statiques.
 */
final class OutilsGabarit {

    private OutilsGabarit() {
    }

    /** Mélange une couleur vers le blanc (fraction 0..1). */
    static int[] melerBlanc(int[] rgb, double fraction) {
        return new int[]{
                arrondi(rgb[0] + (255 - rgb[0]) * fraction),
                arrondi(rgb[1] + (255 - rgb[1]) * fraction),
                arrondi(rgb[2] + (255 - rgb[2]) * fraction)
        };
    }

    /** Mélange une couleur vers le noir (fraction 0..1). */
    static int[] melerNoir(int[] rgb, double fraction) {
        return new int[]{
                arrondi(rgb[0] * (1 - fraction)),
                arrondi(rgb[1] * (1 - fraction)),
                arrondi(rgb[2] * (1 - fraction))
        };
    }

    /** Assombrit une couleur jusqu'à une luminance maximale (teinte gardée). */
    static int[] foncer(int[] rgb, double lumMax) {
        double r = rgb[0], v = rgb[1], b = rgb[2];
        double lum = luminance(r, v, b);
        if (lum > lumMax && lum > 0) {
            double f = lumMax / lum;
            r *= f; v *= f; b *= f;
        }
        return new int[]{arrondi(r), arrondi(v), arrondi(b)};
    }

    static double luminance(int[] rgb) {
        return luminance(rgb[0], rgb[1], rgb[2]);
    }

    /** Un fond est « sombre » s'il appelle du texte clair. */
    static boolean sombre(int[] rgb) {
        return luminance(rgb) < 128;
    }

    /**
     * Rend {@code couleur} lisible sur {@code fond} : si le fond est clair, la
     * couleur est assombrie ; s'il est sombre, elle est éclaircie — jusqu'à un
     * écart de luminance {@code ecart}. La teinte est préservée.
     */
    static int[] contraster(int[] couleur, int[] fond, double ecart) {
        double lf = luminance(fond), lc = luminance(couleur);
        if (lf >= 128) {
            double cible = lf - ecart;
            if (lc > cible) return versLuminance(couleur, Math.max(0, cible));
        } else {
            double cible = lf + ecart;
            if (lc < cible) return versLuminance(couleur, Math.min(255, cible));
        }
        return couleur;
    }

    /** Amène une couleur à une luminance cible (vers le noir ou vers le blanc), teinte gardée. */
    static int[] versLuminance(int[] couleur, double cible) {
        double lc = luminance(couleur);
        if (lc <= 0) {
            int v = arrondi(cible);
            return new int[]{v, v, v};
        }
        if (cible <= lc) {
            double f = cible / lc;
            return new int[]{arrondi(couleur[0] * f), arrondi(couleur[1] * f), arrondi(couleur[2] * f)};
        }
        double f = (cible - lc) / (255 - lc);
        return melerBlanc(couleur, f);
    }

    private static double luminance(double r, double v, double b) {
        return 0.2126 * r + 0.7152 * v + 0.0722 * b;
    }

    private static int arrondi(double x) {
        return (int) Math.round(x);
    }
}
