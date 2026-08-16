package com.printnow.module.studio.service.gabarit;

import com.printnow.module.studio.model.ContenuCarteVisite;

/**
 * Construit le HTML/CSS d'une carte de visite 85 × 55 mm pour une structure
 * donnée (« monogramme », « diagonale », « pleine »). Couleurs et fond viennent
 * des jetons du {@link Style}.
 */
final class CarteHtml {

    private CarteHtml() {
    }

    static String page(ContenuCarteVisite c, Style s, String structure) {
        String corps = switch (structure) {
            case "diagonale" -> corpsDiagonale(c);
            case "pleine" -> corpsPleine(c);
            case "sobre" -> corpsSobre(c);
            case "bandeau" -> corpsBandeau(c);
            case "cote" -> corpsCote(c);
            default -> corpsMonogramme(c);
        };
        return "<!DOCTYPE html>\n<html><head><meta charset=\"UTF-8\"/>\n<style>\n"
                + css(s, structure)
                + "\n</style></head>\n<body class=\"" + structure + "\">\n" + corps + "\n</body></html>";
    }

    // --- corps par structure ---------------------------------------------------

    private static String corpsMonogramme(ContenuCarteVisite c) {
        StringBuilder b = new StringBuilder();
        b.append("<div class=\"ring\"></div>");
        b.append("<div class=\"badge\"><span>").append(esc(initiales(c.nom()))).append("</span></div>");
        b.append("<div class=\"id\"><div class=\"nom\">").append(esc(nz(c.nom()))).append("</div>");
        if (rempli(c.poste())) b.append("<div class=\"poste\">").append(esc(c.poste())).append("</div>");
        b.append("</div>");
        b.append("<div class=\"filet\"></div>");
        if (rempli(c.entreprise())) b.append("<div class=\"entreprise\">").append(esc(c.entreprise())).append("</div>");
        b.append(coord(c));
        return b.toString();
    }

    private static String corpsDiagonale(ContenuCarteVisite c) {
        StringBuilder b = new StringBuilder("<div class=\"top\">");
        b.append("<div class=\"nom\">").append(esc(nz(c.nom()))).append("</div>");
        if (rempli(c.poste())) b.append("<div class=\"poste\">").append(esc(c.poste())).append("</div>");
        if (rempli(c.entreprise())) b.append("<div class=\"entreprise\">").append(esc(c.entreprise())).append("</div>");
        b.append("</div>");
        b.append("<div class=\"band\">").append(coord(c)).append("</div>");
        return b.toString();
    }

    private static String corpsPleine(ContenuCarteVisite c) {
        StringBuilder b = new StringBuilder();
        b.append("<div class=\"ring\"></div>");
        if (rempli(c.poste())) b.append("<div class=\"poste\">").append(esc(c.poste())).append("</div>");
        b.append("<div class=\"nom\">").append(esc(nz(c.nom()))).append("</div>");
        b.append("<div class=\"rule\"></div>");
        if (rempli(c.entreprise())) b.append("<div class=\"entreprise\">").append(esc(c.entreprise())).append("</div>");
        b.append(coord(c));
        return b.toString();
    }

    private static String corpsBandeau(ContenuCarteVisite c) {
        StringBuilder b = new StringBuilder("<div class=\"band\">");
        b.append("<div class=\"nom\">").append(esc(nz(c.nom()))).append("</div>");
        if (rempli(c.poste())) b.append("<div class=\"poste\">").append(esc(c.poste())).append("</div>");
        b.append("</div><div class=\"bot\">");
        if (rempli(c.entreprise())) b.append("<div class=\"entreprise\">").append(esc(c.entreprise())).append("</div>");
        b.append(coord(c)).append("</div>");
        return b.toString();
    }

    private static String corpsCote(ContenuCarteVisite c) {
        StringBuilder b = new StringBuilder("<div class=\"side\">");
        b.append("<div class=\"ini\">").append(esc(initiales(c.nom()))).append("</div>");
        b.append("<div class=\"nom\">").append(esc(nz(c.nom()))).append("</div></div>");
        b.append("<div class=\"main\">");
        if (rempli(c.poste())) b.append("<div class=\"poste\">").append(esc(c.poste())).append("</div>");
        if (rempli(c.entreprise())) b.append("<div class=\"entreprise\">").append(esc(c.entreprise())).append("</div>");
        b.append(coord(c)).append("</div>");
        return b.toString();
    }

    private static String corpsSobre(ContenuCarteVisite c) {
        StringBuilder b = new StringBuilder("<div class=\"nom\">").append(esc(nz(c.nom()))).append("</div>");
        if (rempli(c.poste())) b.append("<div class=\"poste\">").append(esc(c.poste())).append("</div>");
        b.append("<div class=\"rule\"></div>");
        if (rempli(c.entreprise())) b.append("<div class=\"entreprise\">").append(esc(c.entreprise())).append("</div>");
        b.append(coord(c));
        return b.toString();
    }

    private static String coord(ContenuCarteVisite c) {
        StringBuilder b = new StringBuilder("<div class=\"coord\">");
        for (String champ : new String[]{c.telephone(), c.email(), c.siteWeb(), c.adresse()}) {
            if (rempli(champ)) b.append("<div class=\"row\"><span class=\"marq\"></span>").append(esc(champ)).append("</div>");
        }
        return b.append("</div>").toString();
    }

    // --- feuille de style ------------------------------------------------------

    private static String css(Style s, String structure) {
        String police = "moderne".equals(s.police().code()) ? "sans-serif" : "serif";
        String page = rgb(s.page());
        String bloc = rgb(s.bloc());
        String panneau = rgb(s.panneau());
        String titre = rgb(s.titre());
        String accent = rgb(s.accentPage());
        String texteDoux = rgb(s.texteDoux());
        String filet = rgb(s.filet());
        String pastille = rgb(s.pastille());
        String surPastille = rgb(s.surPastille());
        String surB = rgb(s.surBloc());
        String surBd = rgb(s.surBlocDoux());
        String accB = rgb(s.accentBloc());
        String surP = rgb(s.surPanneau());
        String accP = rgb(s.accentPanneau());

        StringBuilder c = new StringBuilder();
        c.append("@page { size: 85mm 55mm; margin: 0; }\n");
        c.append("* { box-sizing: border-box; }\n");
        c.append("body { margin:0; font-family:").append(police).append("; line-height:1.35; }\n");
        c.append(".row { margin-bottom:3px; }\n");
        c.append(".marq { display:inline-block; width:3px; height:3px; border-radius:50%; margin-right:6px; vertical-align:middle; }\n");

        switch (structure) {
            case "diagonale" -> {
                c.append("body { background:").append(page).append("; }\n");
                c.append(".top { padding:16px 18px 0; }\n");
                c.append(".nom { font-weight:bold; color:").append(titre).append("; font-size:15px; }\n");
                c.append(".poste { color:").append(accent).append("; text-transform:uppercase; letter-spacing:1px; font-size:7px; margin-top:3px; }\n");
                c.append(".entreprise { color:").append(texteDoux).append("; font-size:8px; margin-top:3px; }\n");
                c.append(".band { position:fixed; bottom:0; left:0; right:0; background:").append(panneau).append("; padding:11px 18px; }\n");
                c.append(".band .coord { color:").append(surP).append("; font-size:7.5px; }\n");
                c.append(".band .marq { background:").append(accP).append("; }\n");
            }
            case "pleine" -> {
                c.append("body { background:").append(bloc).append("; padding:16px 18px; }\n");
                c.append(".ring { position:fixed; top:-14px; right:-14px; width:40px; height:40px; border:2.5px solid ").append(accB).append("; border-radius:50%; }\n");
                c.append(".poste { color:").append(accB).append("; text-transform:uppercase; letter-spacing:1px; font-size:6.5px; }\n");
                c.append(".nom { font-weight:bold; color:").append(surB).append("; font-size:16px; margin-top:2px; }\n");
                c.append(".rule { width:28px; height:2px; background:").append(accB).append("; margin:8px 0; }\n");
                c.append(".entreprise { font-weight:bold; color:").append(surBd).append("; font-size:8px; margin-bottom:5px; }\n");
                c.append(".coord { color:").append(surBd).append("; font-size:7.5px; }\n");
                c.append(".coord .marq { background:").append(accB).append("; }\n");
            }
            case "sobre" -> {
                c.append("body { background:").append(page).append("; padding:20px 20px; }\n");
                c.append(".nom { font-weight:bold; color:").append(titre).append("; font-size:17px; }\n");
                c.append(".poste { color:").append(accent).append("; text-transform:uppercase; letter-spacing:1.2px; font-size:7px; margin-top:3px; }\n");
                c.append(".rule { width:26px; height:2px; background:").append(accent).append("; margin:10px 0; }\n");
                c.append(".entreprise { font-weight:bold; color:").append(titre).append("; font-size:8.5px; margin-bottom:6px; }\n");
                c.append(".coord { color:").append(texteDoux).append("; font-size:7.5px; }\n");
                c.append(".coord .marq { background:").append(accent).append("; }\n");
            }
            case "bandeau" -> {
                c.append("body { background:").append(page).append("; }\n");
                c.append(".band { background:").append(panneau).append("; padding:14px 18px; }\n");
                c.append(".band .nom { font-weight:bold; color:").append(surP).append("; font-size:15px; }\n");
                c.append(".band .poste { color:").append(accP).append("; text-transform:uppercase; letter-spacing:1px; font-size:7px; margin-top:2px; }\n");
                c.append(".bot { padding:12px 18px; }\n");
                c.append(".bot .entreprise { font-weight:bold; color:").append(titre).append("; font-size:8.5px; margin-bottom:5px; }\n");
                c.append(".coord { color:").append(texteDoux).append("; font-size:7.5px; }\n");
                c.append(".coord .marq { background:").append(accent).append("; }\n");
            }
            case "cote" -> {
                c.append("body { background:").append(page).append("; }\n");
                c.append(".side { position:absolute; left:0; top:0; width:30mm; height:55mm; background:").append(panneau).append("; padding:15px 12px; }\n");
                c.append(".side .ini { color:").append(accP).append("; font-weight:bold; font-size:20px; }\n");
                c.append(".side .nom { color:").append(surP).append("; font-weight:bold; font-size:11px; margin-top:10px; }\n");
                c.append(".main { margin-left:30mm; padding:15px 14px; }\n");
                c.append(".main .poste { color:").append(accent).append("; text-transform:uppercase; letter-spacing:1px; font-size:6.5px; }\n");
                c.append(".main .entreprise { font-weight:bold; color:").append(titre).append("; font-size:8.5px; margin:6px 0; }\n");
                c.append(".coord { color:").append(texteDoux).append("; font-size:7px; margin-top:4px; }\n");
                c.append(".coord .marq { background:").append(accent).append("; }\n");
            }
            default -> { // monogramme
                c.append("body { background:").append(page).append("; padding:15px 18px; }\n");
                c.append(".ring { position:fixed; bottom:-16px; right:-16px; width:38px; height:38px; border:2px solid ").append(accent).append("; border-radius:50%; }\n");
                c.append(".badge { display:inline-block; vertical-align:middle; width:38px; height:38px; background:").append(pastille).append("; border-radius:19px; text-align:center; }\n");
                c.append(".badge span { line-height:38px; color:").append(surPastille).append("; font-weight:bold; font-size:15px; }\n");
                c.append(".id { display:inline-block; vertical-align:middle; margin-left:12px; }\n");
                c.append(".id .nom { font-weight:bold; color:").append(titre).append("; font-size:14px; }\n");
                c.append(".id .poste { color:").append(accent).append("; text-transform:uppercase; letter-spacing:1px; font-size:6.5px; margin-top:2px; }\n");
                c.append(".filet { height:0.8px; background:").append(filet).append("; margin:12px 0 10px; }\n");
                c.append(".entreprise { font-weight:bold; color:").append(titre).append("; font-size:8.5px; margin-bottom:5px; }\n");
                c.append(".coord { color:").append(texteDoux).append("; font-size:7.5px; }\n");
                c.append(".coord .marq { background:").append(accent).append("; }\n");
            }
        }
        return c.toString();
    }

    // --- utilitaires -----------------------------------------------------------

    private static String initiales(String nom) {
        if (nom == null) return "";
        StringBuilder sb = new StringBuilder();
        for (String mot : nom.trim().split("\\s+")) {
            if (!mot.isBlank()) sb.append(Character.toUpperCase(mot.charAt(0)));
            if (sb.length() >= 2) break;
        }
        return sb.toString();
    }

    private static boolean rempli(String s) {
        return s != null && !s.isBlank();
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String rgb(int[] c) {
        return "rgb(" + c[0] + "," + c[1] + "," + c[2] + ")";
    }
}
