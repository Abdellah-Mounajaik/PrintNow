package com.printnow.module.studio.service.gabarit;

import com.printnow.module.studio.model.ContenuCv;

import java.util.ArrayList;
import java.util.List;

/**
 * Construit le HTML/CSS d'un CV pour une structure donnée (« moderne », « entete »,
 * « minimal »). Les couleurs et le fond viennent des jetons du {@link Style} et
 * sont injectés comme valeurs concrètes (OpenHTMLtoPDF ne gère pas var()).
 * Ajouter une structure = ajouter un bloc CSS + un corps ici, sans géométrie.
 */
final class CvHtml {

    private CvHtml() {
    }

    static String page(ContenuCv cv, Style s, String structure) {
        String corps = switch (structure) {
            case "entete" -> corpsEntete(cv, s);
            case "minimal" -> corpsMinimal(cv, s);
            case "bandeau" -> corpsBandeau(cv, s);
            case "timeline", "cartes" -> corpsMinimal(cv, s);   // même corps, CSS différent
            default -> corpsModerne(cv, s);   // moderne, droite
        };
        return "<!DOCTYPE html>\n<html><head><meta charset=\"UTF-8\"/>\n<style>\n"
                + css(s, structure)
                + "\n</style></head>\n<body class=\"" + structure + "\">\n" + corps + "\n</body></html>";
    }

    // --- corps par structure ---------------------------------------------------

    private static String corpsModerne(ContenuCv cv, Style s) {
        StringBuilder aside = new StringBuilder();
        aside.append("<div class=\"mono\"><span>").append(esc(initiales(cv.nom()))).append("</span></div>");
        aside.append("<div class=\"nom\">").append(esc(nz(cv.nom()))).append("</div>");
        if (rempli(cv.titrePro())) aside.append("<div class=\"titrepro\">").append(esc(cv.titrePro())).append("</div>");
        for (String[] sec : new String[][]{
                {"Contact", listeSimple(contactLignes(cv.contact()))},
                {"Compétences", listeSimple(cv.competences())},
                {"Langues", listeSimple(cv.langues())}}) {
            if (!sec[1].isEmpty()) aside.append("<div class=\"seclat\">").append(esc(sec[0])).append("</div>").append(sec[1]);
        }

        StringBuilder main = new StringBuilder();
        main.append(experiences(cv.experiences()));
        main.append(formations(cv.formations()));

        return "<div class=\"aside\">" + aside + "</div><div class=\"main\">" + main + "</div>";
    }

    private static String corpsEntete(ContenuCv cv, Style s) {
        StringBuilder h = new StringBuilder("<div class=\"hdr\">");
        h.append("<div class=\"mono\"><span>").append(esc(initiales(cv.nom()))).append("</span></div>");
        h.append("<div class=\"hdr-txt\"><div class=\"nom\">").append(esc(nz(cv.nom()))).append("</div>");
        if (rempli(cv.titrePro())) h.append("<div class=\"titrepro\">").append(esc(cv.titrePro())).append("</div>");
        String contact = String.join("&#160;&#160;•&#160;&#160;", contactLignes(cv.contact()));
        if (!contact.isBlank()) h.append("<div class=\"contact\">").append(contact).append("</div>");
        h.append("</div></div>");

        StringBuilder b = new StringBuilder("<div class=\"corps\">");
        if (!cv.competences().isEmpty()) b.append(sec("Compétences")).append(chips(cv.competences()));
        b.append(experiences(cv.experiences())).append(formations(cv.formations()));
        if (cv.langues() != null && !cv.langues().isEmpty()) b.append(sec("Langues")).append(chips(cv.langues()));
        b.append("</div>");
        return h + b.toString();
    }

    private static String corpsMinimal(ContenuCv cv, Style s) {
        StringBuilder b = new StringBuilder("<div class=\"corps\">");
        b.append("<div class=\"nom\">").append(esc(nz(cv.nom()))).append("</div>");
        if (rempli(cv.titrePro())) b.append("<div class=\"titrepro\">").append(esc(cv.titrePro())).append("</div>");
        b.append("<div class=\"filet\"></div>");
        String contact = String.join("&#160;&#160;•&#160;&#160;", contactLignes(cv.contact()));
        if (!contact.isBlank()) b.append("<div class=\"contact\">").append(contact).append("</div>");
        if (!cv.competences().isEmpty()) b.append(sec("Compétences")).append(chips(cv.competences()));
        b.append(experiences(cv.experiences())).append(formations(cv.formations()));
        if (cv.langues() != null && !cv.langues().isEmpty()) b.append(sec("Langues")).append(chips(cv.langues()));
        b.append("</div>");
        return b.toString();
    }

    // --- fragments partagés ----------------------------------------------------

    private static String corpsBandeau(ContenuCv cv, Style s) {
        StringBuilder h = new StringBuilder("<div class=\"topbar\">");
        h.append("<div class=\"nom\">").append(esc(nz(cv.nom()))).append("</div>");
        if (rempli(cv.titrePro())) h.append("<div class=\"titrepro\">").append(esc(cv.titrePro())).append("</div>");
        String contact = String.join("&#160;&#160;•&#160;&#160;", contactLignes(cv.contact()));
        if (!contact.isBlank()) h.append("<div class=\"contact\">").append(contact).append("</div>");
        h.append("</div>");

        StringBuilder b = new StringBuilder("<div class=\"corps\">");
        if (cv.competences() != null && !cv.competences().isEmpty()) b.append(sec("Compétences")).append(chips(cv.competences()));
        b.append(experiences(cv.experiences())).append(formations(cv.formations()));
        if (cv.langues() != null && !cv.langues().isEmpty()) b.append(sec("Langues")).append(chips(cv.langues()));
        b.append("</div>");
        return h + b.toString();
    }

    private static String sec(String titre) {
        return "<h2 class=\"sec\">" + esc(titre) + "</h2><div class=\"bar\"></div>";
    }

    private static String chips(List<String> items) {
        if (items == null || items.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("<div class=\"chips\">");
        for (String it : items) sb.append("<span class=\"chip\">").append(esc(it)).append("</span>");
        return sb.append("</div>").toString();
    }

    private static String listeSimple(List<String> items) {
        if (items == null || items.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("<ul class=\"liste\">");
        for (String it : items) sb.append("<li>").append(esc(it)).append("</li>");
        return sb.append("</ul>").toString();
    }

    private static String experiences(List<ContenuCv.Experience> exps) {
        if (exps == null || exps.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(sec("Expériences"));
        for (ContenuCv.Experience e : exps) {
            String titre = joindre(" — ", e.poste(), e.entreprise());
            sb.append("<div class=\"item\">");
            if (!titre.isBlank()) sb.append("<div class=\"item-titre\">").append(esc(titre)).append("</div>");
            if (rempli(e.periode())) sb.append("<div class=\"item-meta\">").append(esc(e.periode())).append("</div>");
            if (rempli(e.description())) sb.append("<div class=\"item-desc\">").append(esc(e.description())).append("</div>");
            sb.append("</div>");
        }
        return sb.toString();
    }

    private static String formations(List<ContenuCv.Formation> forms) {
        if (forms == null || forms.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(sec("Formations"));
        for (ContenuCv.Formation f : forms) {
            String ligne = joindre(" — ", f.diplome(), f.ecole());
            if (rempli(f.annee())) ligne = joindre("  ·  ", ligne, f.annee());
            if (!ligne.isBlank()) sb.append("<div class=\"item item-court\">").append(esc(ligne)).append("</div>");
        }
        return sb.toString();
    }

    private static List<String> contactLignes(ContenuCv.Contact c) {
        List<String> l = new ArrayList<>();
        if (c == null) return l;
        if (rempli(c.email())) l.add(c.email());
        if (rempli(c.telephone())) l.add(c.telephone());
        if (rempli(c.ville())) l.add(c.ville());
        return l;
    }

    // --- feuille de style ------------------------------------------------------

    private static String css(Style s, String structure) {
        String police = "moderne".equals(s.police().code()) ? "sans-serif" : "serif";
        String page = rgb(s.page());
        String panneau = rgb(s.panneau());
        String encre = rgb(s.encre());
        String doux = rgb(s.encreDoux());
        String titre = rgb(s.titre());
        String accent = rgb(s.accentPage());
        String surP = rgb(s.surPanneau());
        String surPd = rgb(s.surPanneauDoux());
        String accP = rgb(s.accentPanneau());
        String chip = rgb(s.chip());
        String chipTxt = rgb(s.chipTexte());
        String filet = rgb(s.filet());

        StringBuilder c = new StringBuilder();
        c.append("@page { size: 210mm 297mm; margin: 0; }\n");
        c.append("* { box-sizing: border-box; }\n");
        c.append("body { margin:0; font-family:").append(police).append("; background:").append(page)
                .append("; color:").append(encre).append("; font-size:11px; line-height:1.4; }\n");
        c.append(".nom { font-weight:bold; color:").append(titre).append("; }\n");
        c.append(".titrepro { color:").append(accent).append("; text-transform:uppercase; letter-spacing:2px; font-size:11px; }\n");
        c.append(".sec { color:").append(titre).append("; text-transform:uppercase; letter-spacing:1.5px; font-size:13px; font-weight:bold; margin:18px 0 6px 0; }\n");
        c.append(".bar { width:34px; height:2.5px; background:").append(accent).append("; margin:0 0 12px 0; }\n");
        c.append(".item { margin-bottom:12px; }\n");
        c.append(".item-titre { font-weight:bold; color:").append(encre).append("; }\n");
        c.append(".item-meta { color:").append(accent).append("; font-size:10px; margin:1px 0 3px 0; }\n");
        c.append(".item-desc { color:").append(encre).append("; }\n");
        c.append(".item-court { color:").append(encre).append("; margin-bottom:6px; }\n");
        c.append(".chips { margin-bottom:6px; }\n");
        c.append(".chip { display:inline-block; background:").append(chip).append("; color:").append(chipTxt)
                .append("; border-radius:5px; padding:4px 10px; font-size:10px; margin:0 6px 6px 0; }\n");
        c.append(".contact { color:").append(doux).append("; font-size:10px; }\n");

        switch (structure) {
            case "entete" -> {
                c.append(".hdr { background:").append(panneau).append("; padding:26px 40px; }\n");
                c.append(".hdr .mono { display:inline-block; vertical-align:middle; width:56px; height:56px; border:2.4px solid ").append(accP)
                        .append("; border-radius:28px; text-align:center; }\n");
                c.append(".hdr .mono span { line-height:56px; font-weight:bold; font-size:22px; color:").append(surP).append("; }\n");
                c.append(".hdr-txt { display:inline-block; vertical-align:middle; margin-left:18px; }\n");
                c.append(".hdr .nom { color:").append(surP).append("; font-size:27px; }\n");
                c.append(".hdr .titrepro { color:").append(accP).append("; }\n");
                c.append(".hdr .contact { color:").append(surPd).append("; margin-top:6px; }\n");
                c.append(".corps { padding:26px 40px; }\n");
            }
            case "minimal" -> minimalBase(c, filet);
            case "timeline" -> {
                minimalBase(c, filet);
                c.append(".item { border-left:2px solid ").append(accent).append("; padding-left:16px; }\n");
                c.append(".item-meta { display:inline-block; background:").append(chip).append("; color:").append(chipTxt)
                        .append("; border-radius:10px; padding:2px 9px; }\n");
            }
            case "cartes" -> {
                minimalBase(c, filet);
                c.append(".item { background:").append(chip).append("; border-radius:10px; border-left:3px solid ").append(accent)
                        .append("; padding:12px 14px; margin-bottom:12px; }\n");
            }
            case "bandeau" -> {
                c.append(".topbar { background:").append(panneau).append("; padding:22px 40px; }\n");
                c.append(".topbar .nom { color:").append(surP).append("; font-size:26px; }\n");
                c.append(".topbar .titrepro { color:").append(accP).append("; margin-top:2px; }\n");
                c.append(".topbar .contact { color:").append(surPd).append("; font-size:10px; margin-top:5px; }\n");
                c.append(".corps { padding:24px 40px; }\n");
            }
            case "droite" -> colonne(c, true, panneau, surP, surPd, accP);
            default -> colonne(c, false, panneau, surP, surPd, accP);   // moderne
        }
        return c.toString();
    }

    /** Base commune aux structures une-colonne épurées (minimal, timeline, cartes). */
    private static void minimalBase(StringBuilder c, String filet) {
        c.append(".corps { padding:52px 50px; }\n");
        c.append(".corps > .nom { font-size:32px; margin-bottom:2px; }\n");
        c.append(".corps > .titrepro { font-size:12px; letter-spacing:3px; }\n");
        c.append(".filet { height:1.2px; background:").append(filet).append("; margin:16px 0; }\n");
    }

    /** Colonne latérale (moderne = gauche, droite = miroir). */
    private static void colonne(StringBuilder c, boolean droite, String panneau, String surP, String surPd, String accP) {
        c.append(".aside { position:absolute; ").append(droite ? "right:0" : "left:0")
                .append("; top:0; width:200px; height:297mm; background:").append(panneau).append("; padding:30px 22px; }\n");
        c.append(".main { ").append(droite ? "margin-right:200px" : "margin-left:200px").append("; padding:34px 34px; }\n");
        c.append(".aside .mono { width:52px; height:52px; border:2.2px solid ").append(accP)
                .append("; border-radius:26px; text-align:center; margin:0 auto 12px auto; }\n");
        c.append(".aside .mono span { line-height:52px; font-weight:bold; font-size:20px; color:").append(surP).append("; }\n");
        c.append(".aside .nom { color:").append(surP).append("; font-size:18px; text-align:center; }\n");
        c.append(".aside .titrepro { color:").append(accP).append("; text-align:center; font-size:9px; margin-top:3px; }\n");
        c.append(".seclat { color:").append(accP).append("; text-transform:uppercase; letter-spacing:1.3px; font-weight:bold; font-size:10px; margin:20px 0 7px 0; padding-bottom:4px; border-bottom:2px solid ").append(accP).append("; }\n");
        c.append(".liste { list-style:none; margin:0; padding:0; }\n");
        c.append(".liste li { color:").append(surPd).append("; font-size:9.5px; margin-bottom:5px; }\n");
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

    private static String joindre(String sep, String a, String b) {
        boolean ra = rempli(a), rb = rempli(b);
        if (ra && rb) return a + sep + b;
        return ra ? a : (rb ? b : "");
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
