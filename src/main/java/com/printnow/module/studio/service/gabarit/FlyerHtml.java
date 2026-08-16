package com.printnow.module.studio.service.gabarit;

import com.printnow.module.studio.model.ContenuFlyer;

import java.util.ArrayList;
import java.util.List;

/**
 * Construit le HTML/CSS d'un flyer A5 pour une structure donnée (« pleine »,
 * « editorial », « split »). Couleurs et fond viennent des jetons du {@link Style}.
 * Ajouter une structure = un bloc CSS + un corps ici.
 */
final class FlyerHtml {

    private FlyerHtml() {
    }

    static String page(ContenuFlyer f, Style s, String structure) {
        String corps = switch (structure) {
            case "editorial" -> corpsEditorial(f);
            case "split" -> corpsSplit(f);
            case "affiche" -> corpsCentre(f);   // titre + blocs centrés, CSS poster
            case "hero" -> corpsHero(f);
            case "numeros" -> corpsNumeros(f);
            default -> corpsPleine(f);
        };
        return "<!DOCTYPE html>\n<html><head><meta charset=\"UTF-8\"/>\n<style>\n"
                + css(s, structure)
                + "\n</style></head>\n<body class=\"" + structure + "\">\n" + corps + "\n</body></html>";
    }

    // --- corps par structure ---------------------------------------------------

    private static String corpsPleine(ContenuFlyer f) {
        StringBuilder b = new StringBuilder();
        b.append("<div class=\"ring\"></div>");
        b.append("<div class=\"wrap\">");
        b.append("<div class=\"titre\">").append(esc(nz(f.titre()))).append("</div>");
        b.append("<div class=\"rule\"></div>");
        if (rempli(f.accroche())) b.append("<div class=\"accroche\">").append(esc(f.accroche())).append("</div>");
        if (f.blocs() != null) {
            for (ContenuFlyer.Bloc bl : f.blocs()) {
                if (!rempli(bl.libelle()) && !rempli(bl.valeur())) continue;
                b.append("<div class=\"chip\">");
                if (rempli(bl.libelle())) b.append("<div class=\"lib\">").append(esc(bl.libelle())).append("</div>");
                if (rempli(bl.valeur())) b.append("<div class=\"val\">").append(esc(bl.valeur())).append("</div>");
                b.append("</div>");
            }
        }
        b.append("</div>");
        b.append(pied(f, "contact"));
        return b.toString();
    }

    private static String corpsEditorial(ContenuFlyer f) {
        StringBuilder b = new StringBuilder();
        b.append("<div class=\"hdr\"><div class=\"titre\">").append(esc(nz(f.titre()))).append("</div></div>");
        b.append("<div class=\"corps\">");
        if (rempli(f.accroche())) {
            b.append("<div class=\"accroche\">").append(esc(f.accroche())).append("</div><div class=\"rule\"></div>");
        }
        b.append(blocsListe(f));
        b.append("</div>");
        b.append(pied(f, "foot"));
        return b.toString();
    }

    private static String corpsSplit(ContenuFlyer f) {
        StringBuilder b = new StringBuilder();
        b.append("<div class=\"top\">");
        b.append("<div class=\"titre\">").append(esc(nz(f.titre()))).append("</div>");
        b.append("<div class=\"rule\"></div>");
        if (rempli(f.accroche())) b.append("<div class=\"accroche\">").append(esc(f.accroche())).append("</div>");
        b.append("</div>");
        b.append("<div class=\"corps\">").append(blocsListe(f)).append("</div>");
        b.append(pied(f, "contact"));
        return b.toString();
    }

    // --- fragments -------------------------------------------------------------

    private static String corpsHero(ContenuFlyer f) {
        StringBuilder b = new StringBuilder("<div class=\"top\"><div class=\"titre\">")
                .append(esc(nz(f.titre()))).append("</div></div><div class=\"corps\">");
        if (rempli(f.accroche())) b.append("<div class=\"accroche\">").append(esc(f.accroche())).append("</div>");
        b.append(blocsNumerotes(f)).append("</div>");
        b.append(pied(f, "contact"));
        return b.toString();
    }

    private static String corpsNumeros(ContenuFlyer f) {
        StringBuilder b = new StringBuilder("<div class=\"head\"><div class=\"titre\">")
                .append(esc(nz(f.titre()))).append("</div><div class=\"rule\"></div>");
        if (rempli(f.accroche())) b.append("<div class=\"accroche\">").append(esc(f.accroche())).append("</div>");
        b.append("</div><div class=\"corps\">").append(blocsNumerotes(f)).append("</div>");
        b.append(pied(f, "contact"));
        return b.toString();
    }

    private static String blocsNumerotes(ContenuFlyer f) {
        if (f.blocs() == null) return "";
        StringBuilder b = new StringBuilder();
        int i = 1;
        for (ContenuFlyer.Bloc bl : f.blocs()) {
            if (!rempli(bl.libelle()) && !rempli(bl.valeur())) continue;
            b.append("<div class=\"nbloc\"><span class=\"num\">").append(String.format("%02d", i++))
                    .append("</span><div class=\"txt\">");
            if (rempli(bl.libelle())) b.append("<div class=\"lib\">").append(esc(bl.libelle())).append("</div>");
            if (rempli(bl.valeur())) b.append("<div class=\"val\">").append(esc(bl.valeur())).append("</div>");
            b.append("</div></div>");
        }
        return b.toString();
    }

    private static String corpsCentre(ContenuFlyer f) {
        StringBuilder b = new StringBuilder("<div class=\"wrap\">");
        b.append("<div class=\"titre\">").append(esc(nz(f.titre()))).append("</div>");
        b.append("<div class=\"rule\"></div>");
        if (rempli(f.accroche())) b.append("<div class=\"accroche\">").append(esc(f.accroche())).append("</div>");
        if (f.blocs() != null) {
            for (ContenuFlyer.Bloc bl : f.blocs()) {
                if (!rempli(bl.libelle()) && !rempli(bl.valeur())) continue;
                b.append("<div class=\"cbloc\">");
                if (rempli(bl.libelle())) b.append("<div class=\"lib\">").append(esc(bl.libelle())).append("</div>");
                if (rempli(bl.valeur())) b.append("<div class=\"val\">").append(esc(bl.valeur())).append("</div>");
                b.append("</div>");
            }
        }
        b.append("</div>");
        b.append(pied(f, "contact"));
        return b.toString();
    }

    private static String blocsListe(ContenuFlyer f) {
        if (f.blocs() == null) return "";
        StringBuilder b = new StringBuilder();
        for (ContenuFlyer.Bloc bl : f.blocs()) {
            if (!rempli(bl.libelle()) && !rempli(bl.valeur())) continue;
            b.append("<div class=\"bloc\">");
            if (rempli(bl.libelle()))
                b.append("<span class=\"marq\"></span><span class=\"lib\">").append(esc(bl.libelle())).append("</span>");
            if (rempli(bl.valeur())) b.append("<div class=\"val\">").append(esc(bl.valeur())).append("</div>");
            b.append("</div>");
        }
        return b.toString();
    }

    private static String pied(ContenuFlyer f, String cls) {
        List<String> lignes = new ArrayList<>();
        ContenuFlyer.Contact c = f.contact();
        if (c != null) {
            if (rempli(c.adresse())) lignes.add(esc(c.adresse()));
            List<String> bas = new ArrayList<>();
            if (rempli(c.telephone())) bas.add(esc(c.telephone()));
            if (rempli(c.email())) bas.add(esc(c.email()));
            if (!bas.isEmpty()) lignes.add(String.join("&#160;&#160;•&#160;&#160;", bas));
        }
        if (lignes.isEmpty()) return "";
        return "<div class=\"" + cls + "\">" + String.join("<br/>", lignes) + "</div>";
    }

    // --- feuille de style ------------------------------------------------------

    private static String css(Style s, String structure) {
        String police = "moderne".equals(s.police().code()) ? "sans-serif" : "serif";
        String page = rgb(s.page());
        String bloc = rgb(s.bloc());
        String panneau = rgb(s.panneau());
        String encre = rgb(s.encre());
        String titre = rgb(s.titre());
        String accent = rgb(s.accentPage());
        String surB = rgb(s.surBloc());
        String surBd = rgb(s.surBlocDoux());
        String accB = rgb(s.accentBloc());
        String chip = rgb(s.chipSurBloc());
        String surP = rgb(s.surPanneau());
        String surPd = rgb(s.surPanneauDoux());
        String accP = rgb(s.accentPanneau());

        StringBuilder c = new StringBuilder();
        c.append("@page { size: 148mm 210mm; margin: 0; }\n");
        c.append("* { box-sizing: border-box; }\n");
        c.append("body { margin:0; font-family:").append(police).append("; font-size:12px; line-height:1.4; }\n");

        switch (structure) {
            case "editorial" -> {
                c.append("body { background:").append(page).append("; color:").append(encre).append("; }\n");
                c.append(".hdr { background:").append(panneau).append("; padding:46px 40px; }\n");
                c.append(".hdr .titre { color:").append(surP).append("; font-size:40px; font-weight:bold; line-height:1.05; }\n");
                c.append(".corps { padding:36px 40px; }\n");
                c.append(".accroche { color:").append(titre).append("; font-weight:bold; font-size:18px; }\n");
                c.append(".rule { width:52px; height:3px; background:").append(accent).append("; margin:12px 0 28px; }\n");
                c.append(".bloc { margin-bottom:20px; }\n");
                c.append(".marq { display:inline-block; width:9px; height:9px; background:").append(accent).append("; margin-right:12px; }\n");
                c.append(".lib { font-weight:bold; color:").append(titre).append("; font-size:17px; }\n");
                c.append(".val { color:").append(encre).append("; margin:3px 0 0 21px; font-size:13.5px; }\n");
                c.append(".foot { position:fixed; bottom:0; left:0; right:0; background:").append(panneau)
                        .append("; color:").append(surPd).append("; text-align:center; padding:20px; font-size:10.5px; }\n");
            }
            case "split" -> {
                c.append("body { background:").append(page).append("; color:").append(encre).append("; }\n");
                c.append(".top { background:").append(panneau).append("; padding:52px 40px 40px; }\n");
                c.append(".top .titre { color:").append(surP).append("; font-size:44px; font-weight:bold; line-height:1.03; }\n");
                c.append(".top .rule { width:60px; height:3.5px; background:").append(accP).append("; margin:18px 0 18px; }\n");
                c.append(".top .accroche { color:").append(surPd).append("; font-size:15px; }\n");
                c.append(".corps { padding:34px 40px; }\n");
                c.append(".bloc { margin-bottom:20px; }\n");
                c.append(".marq { display:inline-block; width:9px; height:9px; border-radius:50%; background:").append(accent).append("; margin-right:12px; }\n");
                c.append(".lib { font-weight:bold; color:").append(titre).append("; font-size:18px; }\n");
                c.append(".val { color:").append(accent).append("; margin:3px 0 0 21px; font-size:13.5px; }\n");
                c.append(".contact { position:fixed; bottom:34px; left:0; right:0; text-align:center; color:").append(rgb(s.encreDoux())).append("; font-size:10px; }\n");
            }
            case "affiche" -> {
                c.append("body { background:").append(page).append("; color:").append(encre).append("; text-align:center; }\n");
                c.append(".wrap { padding:64px 30px; }\n");
                c.append(".wrap .titre { color:").append(titre).append("; font-size:46px; font-weight:bold; line-height:1.05; }\n");
                c.append(".wrap .rule { width:70px; height:4px; background:").append(accent).append("; margin:20px auto 26px; }\n");
                c.append(".wrap .accroche { color:").append(rgb(s.encreDoux())).append("; font-size:15px; margin-bottom:30px; }\n");
                c.append(".cbloc { display:inline-block; vertical-align:top; border:2px solid ").append(accent).append("; border-radius:20px; padding:12px 20px; margin:0 5px 12px; }\n");
                c.append(".cbloc .lib { font-weight:bold; color:").append(titre).append("; font-size:14px; }\n");
                c.append(".cbloc .val { color:").append(accent).append("; font-size:13px; margin-top:2px; }\n");
                c.append(".contact { position:fixed; bottom:34px; left:0; right:0; text-align:center; color:").append(rgb(s.encreDoux())).append("; font-size:10px; }\n");
            }
            case "hero" -> {
                c.append("body { background:").append(page).append("; color:").append(encre).append("; }\n");
                c.append(".top { height:85mm; background:").append(panneau).append("; padding:56px 36px 0; }\n");
                c.append(".top .titre { color:").append(surP).append("; font-size:46px; font-weight:bold; line-height:1.02; }\n");
                c.append(".corps { padding:28px 36px; }\n");
                c.append(".corps .accroche { color:").append(rgb(s.encreDoux())).append("; font-size:14px; margin-bottom:22px; }\n");
                c.append(".nbloc { margin-bottom:16px; }\n");
                c.append(".nbloc .num { display:inline-block; vertical-align:top; color:").append(accent).append("; font-weight:bold; font-size:28px; width:44px; }\n");
                c.append(".nbloc .txt { display:inline-block; vertical-align:top; }\n");
                c.append(".nbloc .lib { font-weight:bold; color:").append(titre).append("; font-size:15px; }\n");
                c.append(".nbloc .val { color:").append(accent).append("; font-size:13px; margin-top:1px; }\n");
                c.append(".contact { position:fixed; bottom:26px; left:36px; right:36px; color:").append(rgb(s.encreDoux())).append("; font-size:10px; }\n");
            }
            case "numeros" -> {
                c.append("body { background:").append(page).append("; color:").append(encre).append("; }\n");
                c.append(".head { padding:48px 40px 0; }\n");
                c.append(".head .titre { color:").append(titre).append("; font-size:46px; font-weight:bold; line-height:1.02; }\n");
                c.append(".head .rule { width:60px; height:4px; background:").append(accent).append("; margin:18px 0 16px; }\n");
                c.append(".head .accroche { color:").append(rgb(s.encreDoux())).append("; font-size:14px; }\n");
                c.append(".corps { padding:22px 40px; }\n");
                c.append(".nbloc { margin-bottom:18px; }\n");
                c.append(".nbloc .num { display:inline-block; vertical-align:top; color:").append(accent).append("; font-weight:bold; font-size:32px; width:52px; }\n");
                c.append(".nbloc .txt { display:inline-block; vertical-align:top; }\n");
                c.append(".nbloc .lib { font-weight:bold; color:").append(titre).append("; font-size:16px; }\n");
                c.append(".nbloc .val { color:").append(accent).append("; font-size:14px; margin-top:1px; }\n");
                c.append(".contact { position:fixed; bottom:30px; left:40px; right:40px; color:").append(rgb(s.encreDoux())).append("; font-size:10px; }\n");
            }
            default -> { // pleine
                c.append("body { background:").append(bloc).append("; color:").append(surB).append("; text-align:center; }\n");
                c.append(".ring { position:absolute; top:20px; right:18px; width:56px; height:56px; border:3px solid ").append(accB).append("; border-radius:50%; }\n");
                c.append(".wrap { padding:72px 34px 40px; }\n");
                c.append(".titre { font-size:46px; font-weight:bold; line-height:1.03; color:").append(surB).append("; }\n");
                c.append(".rule { width:70px; height:4px; background:").append(accB).append("; margin:20px auto 28px; }\n");
                c.append(".accroche { color:").append(surBd).append("; font-size:15px; margin-bottom:30px; }\n");
                c.append(".chip { background:").append(chip).append("; border-radius:12px; padding:18px 20px; margin:0 auto 16px; width:80%; }\n");
                c.append(".chip .lib { color:").append(accB).append("; font-weight:bold; font-size:17px; }\n");
                c.append(".chip .val { color:").append(surB).append("; font-size:14px; margin-top:3px; }\n");
                c.append(".contact { position:fixed; bottom:34px; left:0; right:0; text-align:center; color:").append(surBd).append("; font-size:10px; }\n");
            }
        }
        return c.toString();
    }

    // --- utilitaires -----------------------------------------------------------

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
