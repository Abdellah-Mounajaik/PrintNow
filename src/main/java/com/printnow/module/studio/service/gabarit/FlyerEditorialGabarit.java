package com.printnow.module.studio.service.gabarit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.printnow.module.studio.enums.TypeSupport;
import com.printnow.module.studio.model.ContenuFlyer;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.printnow.module.order.service.pdf.PdfFactureHelpers.Cursor;
import static com.printnow.module.order.service.pdf.PdfFactureHelpers.nonVide;
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.envelopper;
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.motLePlusLong;
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.rempli;
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.tailleQuiTient;

/**
 * Maquette « éditoriale » (moderne) : bandeau de titre en haut, corps aéré sur la
 * page (accroche + blocs en liste à marqueurs d'accent), bandeau de contact en
 * pied. Allure magazine.
 */
@Component
@RequiredArgsConstructor
public class FlyerEditorialGabarit implements Gabarit {

    public static final String CODE = "flyer-editorial";

    private static final float LARGEUR = PDRectangle.A5.getWidth();
    private static final float HAUTEUR = PDRectangle.A5.getHeight();
    private static final float X = 40;
    private static final float HEAD_H = HAUTEUR * 0.24f;
    private static final float FOOT_H = HAUTEUR * 0.15f;

    private final ObjectMapper mapper;

    @Override
    public TypeSupport type() {
        return TypeSupport.FLYER;
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public String promptSysteme() {
        return ContenuFlyer.PROMPT;
    }

    @Override
    public byte[] rendre(String json, Style style) throws Exception {
        ContenuFlyer flyer = mapper.readValue(json, ContenuFlyer.class);
        if (flyer == null || (!rempli(flyer.titre()) && !rempli(flyer.accroche()))) {
            throw new IllegalArgumentException("JSON du flyer vide ou inexploitable");
        }
        return dessiner(flyer, style);
    }

    private byte[] dessiner(ContenuFlyer flyer, Style s) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A5);
            document.addPage(page);

            int[] panneau = s.panneau();

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                Cursor c = new Cursor(cs, HAUTEUR);

                c.bandeau(0, 0, LARGEUR, HAUTEUR, s.page());                            // fond de page
                c.bandeau(0, HAUTEUR - HEAD_H, LARGEUR, HEAD_H, panneau);               // bandeau de titre
                String titre = nonVide(flyer.titre());
                float tTitre = tailleQuiTient(s.gras(), motLePlusLong(titre), 30, 18, LARGEUR - 2 * X);
                float y = HAUTEUR - 44;
                for (String ligne : envelopper(s.gras(), tTitre, titre, LARGEUR - 2 * X)) {
                    c.texteCouleurA(s.gras(), tTitre, X, y, ligne, s.surPanneau());
                    y -= tTitre + 4;
                }

                // Corps : accroche + blocs
                float yb = HAUTEUR - HEAD_H - 34;
                if (rempli(flyer.accroche())) {
                    for (String ligne : envelopper(s.gras(), 14, flyer.accroche(), LARGEUR - 2 * X)) {
                        c.texteCouleurA(s.gras(), 14, X, yb, ligne, s.titre());
                        yb -= 19;
                    }
                    yb -= 4;
                    c.bandeau(X, yb, 40, 2.5f, s.accentPage());
                    yb -= 24;
                }

                if (flyer.blocs() != null) {
                    for (ContenuFlyer.Bloc bloc : flyer.blocs()) {
                        if (rempli(bloc.libelle())) {
                            c.bandeau(X, yb - 1, 7, 7, s.accentPage());   // marqueur carré
                            c.texteCouleurA(s.gras(), 13, X + 16, yb, bloc.libelle(), s.titre());
                            yb -= 17;
                        }
                        if (rempli(bloc.valeur())) {
                            c.texteCouleurA(s.normal(), 12.5f, X + 16, yb, bloc.valeur(), s.encre());
                            yb -= 22;
                        } else {
                            yb -= 6;
                        }
                    }
                }

                // Bandeau de contact en pied
                c.bandeau(0, 0, LARGEUR, FOOT_H, panneau);
                List<String> lignes = contact(flyer.contact());
                float yPied = FOOT_H / 2f + (lignes.size() - 1) * 7.5f;
                for (String ligne : lignes) {
                    centre(c, s.normal(), 9.5f, yPied, ligne, s.surPanneauDoux());
                    yPied -= 15;
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Échec du rendu du flyer", e);
        }
    }

    private List<String> contact(ContenuFlyer.Contact contact) {
        List<String> lignes = new ArrayList<>();
        if (contact == null) return lignes;
        List<String> haut = new ArrayList<>();
        if (rempli(contact.telephone())) haut.add(contact.telephone());
        if (rempli(contact.email())) haut.add(contact.email());
        if (!haut.isEmpty()) lignes.add(String.join("   •   ", haut));
        if (rempli(contact.adresse())) lignes.add(contact.adresse());
        return lignes;
    }

    private void centre(Cursor c, PDType1Font font, float taille, float yAbsolu, String valeur, int[] rgb) throws IOException {
        float w = font.getStringWidth(valeur) / 1000f * taille;
        c.texteCouleurA(font, taille, (LARGEUR - w) / 2f, yAbsolu, valeur, rgb);
    }
}
