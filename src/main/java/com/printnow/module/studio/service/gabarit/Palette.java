package com.printnow.module.studio.service.gabarit;

/**
 * Une palette de couleurs appliquée à un support : une couleur principale (titres),
 * une couleur d'accent (valeurs, sous-titres) et une couleur de texte discret.
 * Composantes RVB 0..255.
 */
public enum Palette {
    SOBRE("sobre", new int[]{27, 41, 75}, new int[]{245, 159, 10}, new int[]{110, 110, 120}),
    CHALEUREUX("chaleureux", new int[]{140, 35, 35}, new int[]{225, 120, 40}, new int[]{110, 90, 85}),
    FRAIS("frais", new int[]{18, 78, 92}, new int[]{35, 165, 140}, new int[]{95, 110, 115});

    private final String code;
    private final int[] primaire;
    private final int[] accent;
    private final int[] texte;

    Palette(String code, int[] primaire, int[] accent, int[] texte) {
        this.code = code;
        this.primaire = primaire;
        this.accent = accent;
        this.texte = texte;
    }

    public String code() { return code; }
    public int[] primaire() { return primaire; }
    public int[] accent() { return accent; }
    public int[] texte() { return texte; }
}
