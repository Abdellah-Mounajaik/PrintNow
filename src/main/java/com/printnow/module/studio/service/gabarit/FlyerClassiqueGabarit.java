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
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.rempli;
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.tailleQuiTient;

/**
 * Maquette « classique » : bandeau de titre coloré en haut (hauteur ajustée à
 * l'accroche), blocs d'information centrés sur fond blanc, contact en pied.
 */
@Component
@RequiredArgsConstructor
public class FlyerClassiqueGabarit implements Gabarit {

    public static final String CODE = "flyer-classique";

    private static final int[] BLANC = {255, 255, 255};
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

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                Cursor c = new Cursor(cs, HAUTEUR);

                // Bandeau de titre : hauteur adaptée à l'accroche, découpée pour ne pas déborder.
                List<String> lignesAccroche = envelopper(s.normal(), 12, nonVide(flyer.accroche()), LARGEUR - 80);
                float hBandeau = 78 + lignesAccroche.size() * 16;
                c.bandeau(0, HAUTEUR - hBandeau, LARGEUR, hBandeau, s.primaire());

                float y = HAUTEUR - 46;
                float tailleTitre = tailleQuiTient(s.gras(), nonVide(flyer.titre()), 26, 14, LARGEUR - 50);
                centre(c, s.gras(), tailleTitre, y, nonVide(flyer.titre()), BLANC);
                y -= 30;
                for (String ligne : lignesAccroche) {
                    centre(c, s.normal(), 12, y, ligne, BLANC);
                    y -= 16;
                }

                // Blocs d'information au centre
                c.y = HAUTEUR - hBandeau - 50;
                if (flyer.blocs() != null) {
                    for (ContenuFlyer.Bloc bloc : flyer.blocs()) {
                        if (rempli(bloc.libelle())) {
                            centre(c, s.gras(), 15, c.y, nonVide(bloc.libelle()), s.primaire());
                            c.avancer(22);
                        }
                        if (rempli(bloc.valeur())) {
                            centre(c, s.normal(), 13, c.y, bloc.valeur(), s.accent());
                            c.avancer(30);
                        } else {
                            c.avancer(8);
                        }
                    }
                }

                // Contact en pied, empilé
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
        float largeurTexte = font.getStringWidth(valeur) / 1000f * taille;
        c.texteCouleurA(font, taille, (LARGEUR - largeurTexte) / 2f, yAbsolu, valeur, rgb);
    }
}
