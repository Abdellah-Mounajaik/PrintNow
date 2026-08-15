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
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.eclaircir;
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.envelopper;
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.foncer;
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.motLePlusLong;
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.rempli;
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.tailleQuiTient;

/**
 * Maquette « diagonale » (moderne) : bloc coloré en haut à bord inférieur incliné,
 * titre et accroche en blanc, blocs à puces sur blanc, motif d'anneau en coin.
 */
@Component
@RequiredArgsConstructor
public class FlyerDiagonaleGabarit implements Gabarit {

    public static final String CODE = "flyer-diagonale";

    private static final int[] BLANC = {255, 255, 255};
    private static final int[] CLAIR = {224, 228, 236};
    private static final float LARGEUR = PDRectangle.A5.getWidth();
    private static final float HAUTEUR = PDRectangle.A5.getHeight();
    private static final float X = 38;

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

            int[] fond = foncer(s.primaire(), 76);
            int[] accentClair = eclaircir(s.accent(), 200);
            float gy = HAUTEUR * 0.60f;   // bord incliné, côté gauche (bas)
            float dy = HAUTEUR * 0.50f;   // bord incliné, côté droit (haut)

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                Cursor c = new Cursor(cs, HAUTEUR);

                // Bloc coloré en haut, bord inférieur incliné
                Formes.polygone(cs, new float[]{0, LARGEUR, LARGEUR, 0}, new float[]{HAUTEUR, HAUTEUR, dy, gy}, fond);
                // Fin liseré d'accent sous l'inclinaison
                Formes.polygone(cs, new float[]{0, LARGEUR, LARGEUR, 0}, new float[]{gy, dy, dy - 4, gy - 4}, s.accent());
                // Motif anneau en coin haut-droit
                Formes.anneau(cs, LARGEUR - 10, HAUTEUR - 12, 24, 3f, accentClair);

                // Titre + accroche, en blanc sur le bloc
                String titre = nonVide(flyer.titre());
                float tTitre = tailleQuiTient(s.gras(), motLePlusLong(titre), 34, 20, LARGEUR - 2 * X);
                float y = HAUTEUR - 92;
                for (String ligne : envelopper(s.gras(), tTitre, titre, LARGEUR - 2 * X)) {
                    c.texteCouleurA(s.gras(), tTitre, X, y, ligne, BLANC);
                    y -= tTitre + 6;
                }
                y -= 8;
                for (String ligne : envelopper(s.normal(), 13, nonVide(flyer.accroche()), LARGEUR - 2 * X)) {
                    c.texteCouleurA(s.normal(), 13, X, y, ligne, CLAIR);
                    y -= 18;
                }

                // Blocs à puces, sur blanc
                float yb = gy - 40;
                if (flyer.blocs() != null) {
                    for (ContenuFlyer.Bloc bloc : flyer.blocs()) {
                        if (rempli(bloc.libelle())) {
                            Formes.disque(cs, X + 3, yb + 4f, 2.4f, s.accent());
                            c.texteCouleurA(s.gras(), 14, X + 16, yb, bloc.libelle(), s.primaire());
                            yb -= 19;
                        }
                        if (rempli(bloc.valeur())) {
                            c.texteCouleurA(s.normal(), 13, X + 16, yb, bloc.valeur(), s.accent());
                            yb -= 26;
                        } else {
                            yb -= 8;
                        }
                    }
                }

                // Contact en pied, centré
                List<String> lignes = contact(flyer.contact());
                float yPied = 40 + (lignes.size() - 1) * 15f;
                for (String ligne : lignes) {
                    centre(c, s.normal(), 10, yPied, ligne, s.texte());
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
        if (rempli(contact.adresse())) lignes.add(contact.adresse());
        List<String> bas = new ArrayList<>();
        if (rempli(contact.telephone())) bas.add(contact.telephone());
        if (rempli(contact.email())) bas.add(contact.email());
        if (!bas.isEmpty()) lignes.add(String.join("   •   ", bas));
        return lignes;
    }

    private void centre(Cursor c, PDType1Font font, float taille, float yAbsolu, String valeur, int[] rgb) throws IOException {
        float w = font.getStringWidth(valeur) / 1000f * taille;
        c.texteCouleurA(font, taille, (LARGEUR - w) / 2f, yAbsolu, valeur, rgb);
    }
}
