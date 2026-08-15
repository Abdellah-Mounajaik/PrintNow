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
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.melerBlanc;
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.motLePlusLong;
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.rempli;
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.tailleQuiTient;

/**
 * Maquette « pleine couleur » (moderne) : fond coloré plein, titre centré souligné
 * d'un trait d'accent, blocs présentés en cartouches arrondis, motifs géométriques.
 */
@Component
@RequiredArgsConstructor
public class FlyerPleineGabarit implements Gabarit {

    public static final String CODE = "flyer-pleine";

    private static final int[] BLANC = {255, 255, 255};
    private static final int[] CLAIR = {226, 229, 236};
    private static final float LARGEUR = PDRectangle.A5.getWidth();
    private static final float HAUTEUR = PDRectangle.A5.getHeight();

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

            int[] fond = foncer(s.primaire(), 78);
            int[] panneau = melerBlanc(fond, 0.14);   // cartouches un peu plus clairs que le fond
            int[] accentClair = eclaircir(s.accent(), 205);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                Cursor c = new Cursor(cs, HAUTEUR);
                c.bandeau(0, 0, LARGEUR, HAUTEUR, fond);

                // Motifs : anneau en coin, petite grappe de points
                Formes.anneau(cs, LARGEUR - 18, HAUTEUR - 22, 24, 3f, accentClair);
                Formes.disque(cs, 30, HAUTEUR - 28, 4, accentClair);
                Formes.disque(cs, 45, HAUTEUR - 28, 4, accentClair);
                Formes.disque(cs, 37, HAUTEUR - 16, 4, accentClair);

                // Titre centré
                String titre = nonVide(flyer.titre());
                float tTitre = tailleQuiTient(s.gras(), motLePlusLong(titre), 34, 20, LARGEUR - 70);
                float y = HAUTEUR - 108;
                for (String ligne : envelopper(s.gras(), tTitre, titre, LARGEUR - 70)) {
                    centre(c, s.gras(), tTitre, y, ligne, BLANC);
                    y -= tTitre + 6;
                }
                // Trait d'accent centré
                c.bandeau(LARGEUR / 2f - 28, y - 2, 56, 3, accentClair);
                y -= 26;
                // Accroche centrée
                for (String ligne : envelopper(s.normal(), 13, nonVide(flyer.accroche()), LARGEUR - 90)) {
                    centre(c, s.normal(), 13, y, ligne, CLAIR);
                    y -= 18;
                }

                // Blocs en cartouches arrondis
                y -= 18;
                float chipX = 48, chipW = LARGEUR - 96, chipH = 46;
                if (flyer.blocs() != null) {
                    for (ContenuFlyer.Bloc bloc : flyer.blocs()) {
                        float chipY = y - chipH;
                        Formes.rectArrondi(cs, chipX, chipY, chipW, chipH, 8, panneau);
                        if (rempli(bloc.libelle())) {
                            centre(c, s.gras(), 13, chipY + chipH - 18, bloc.libelle(), accentClair);
                        }
                        if (rempli(bloc.valeur())) {
                            centre(c, s.normal(), 13, chipY + 12, bloc.valeur(), BLANC);
                        }
                        y = chipY - 14;
                    }
                }

                // Contact en pied
                List<String> lignes = contact(flyer.contact());
                float yPied = 40 + (lignes.size() - 1) * 15f;
                for (String ligne : lignes) {
                    centre(c, s.normal(), 10, yPied, ligne, CLAIR);
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
