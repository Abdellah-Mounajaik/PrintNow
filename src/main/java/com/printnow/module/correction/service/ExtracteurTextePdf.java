package com.printnow.module.correction.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Extraction du texte d'un PDF, page par page, en conservant la position à
 * l'écran de chaque caractère. Ces positions servent à placer les surlignages
 * sur les fautes qui n'ont pas pu être corrigées directement.
 */
public class ExtracteurTextePdf {

    /** Position d'un mot sur la page, en coordonnées PDF. */
    public record PositionMot(String mot, float x, float y, float largeur, float hauteur) {}

    /**
     * @return le texte de chaque page (index 0 = page 1)
     */
    public static List<String> extraireParPage(PDDocument document) throws IOException {
        List<String> pages = new ArrayList<>();
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setSortByPosition(true);

        for (int i = 1; i <= document.getNumberOfPages(); i++) {
            stripper.setStartPage(i);
            stripper.setEndPage(i);
            pages.add(stripper.getText(document));
        }
        return pages;
    }

    /**
     * Localise les mots d'une page. Utilisé pour retrouver où surligner une faute.
     *
     * @return les positions, regroupées par mot (un même mot peut apparaître plusieurs fois)
     */
    public static Map<String, List<PositionMot>> positionsDesMots(PDDocument document, int page) throws IOException {
        Map<String, List<PositionMot>> positions = new HashMap<>();

        PDFTextStripper stripper = new PDFTextStripper() {
            @Override
            protected void writeString(String texte, List<TextPosition> positionsTexte) {
                StringBuilder motCourant = new StringBuilder();
                List<TextPosition> lettresDuMot = new ArrayList<>();

                for (TextPosition lettre : positionsTexte) {
                    String c = lettre.getUnicode();
                    if (c.isBlank()) {
                        enregistrer(motCourant, lettresDuMot, positions);
                    } else {
                        motCourant.append(c);
                        lettresDuMot.add(lettre);
                    }
                }
                enregistrer(motCourant, lettresDuMot, positions);
            }
        };
        stripper.setSortByPosition(true);
        stripper.setStartPage(page);
        stripper.setEndPage(page);
        stripper.getText(document);

        return positions;
    }

    private static void enregistrer(StringBuilder motCourant,
                                    List<TextPosition> lettres,
                                    Map<String, List<PositionMot>> positions) {
        if (motCourant.length() > 0 && !lettres.isEmpty()) {
            TextPosition premiere = lettres.get(0);
            TextPosition derniere = lettres.get(lettres.size() - 1);

            float x = premiere.getXDirAdj();
            float droite = derniere.getXDirAdj() + derniere.getWidthDirAdj();
            float hauteur = premiere.getHeightDir();
            // PDFTextStripper donne le haut du texte ; les annotations attendent
            // une origine en bas de page, d'où la conversion.
            float y = premiere.getPageHeight() - premiere.getYDirAdj();

            String mot = nettoyer(motCourant.toString());
            if (!mot.isEmpty()) {
                positions.computeIfAbsent(mot, ignore -> new ArrayList<>())
                        .add(new PositionMot(mot, x, y, droite - x, hauteur));
            }
        }
        motCourant.setLength(0);
        lettres.clear();
    }

    /** Retire la ponctuation collée au mot, que LanguageTool ne renvoie pas. */
    private static String nettoyer(String mot) {
        return mot.replaceAll("^[\\p{Punct}«»…]+|[\\p{Punct}«»…]+$", "");
    }
}
