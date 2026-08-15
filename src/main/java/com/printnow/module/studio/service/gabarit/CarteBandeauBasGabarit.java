package com.printnow.module.studio.service.gabarit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.printnow.module.studio.enums.TypeSupport;
import com.printnow.module.studio.model.ContenuCarteVisite;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static com.printnow.module.order.service.pdf.PdfFactureHelpers.Cursor;
import static com.printnow.module.order.service.pdf.PdfFactureHelpers.nonVide;
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.foncer;
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.rempli;

/**
 * Maquette « bandeau bas » : identité (nom, fonction, entreprise) en haut sur
 * fond blanc ; coordonnées en blanc sur une bande colorée en pied.
 */
@Component
@RequiredArgsConstructor
public class CarteBandeauBasGabarit implements Gabarit {

    public static final String CODE = "carte-bandeau";

    private static final float MM = 72f / 25.4f;
    private static final float LARGEUR = 85 * MM;
    private static final float HAUTEUR = 55 * MM;
    private static final float X = 18;
    private static final float BANDE_H = HAUTEUR * 0.42f;

    private static final int[] CLAIR = {226, 229, 236};

    private final ObjectMapper mapper;

    @Override
    public TypeSupport type() {
        return TypeSupport.CARTE_VISITE;
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public String promptSysteme() {
        return ContenuCarteVisite.PROMPT;
    }

    @Override
    public byte[] rendre(String json, Style style) throws Exception {
        ContenuCarteVisite carte = mapper.readValue(json, ContenuCarteVisite.class);
        if (carte == null || (!rempli(carte.nom()) && !rempli(carte.entreprise()))) {
            throw new IllegalArgumentException("JSON de la carte vide ou inexploitable");
        }
        return dessiner(carte, style);
    }

    private byte[] dessiner(ContenuCarteVisite carte, Style s) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(new PDRectangle(LARGEUR, HAUTEUR));
            document.addPage(page);

            int[] fond = foncer(s.primaire(), 74);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                Cursor c = new Cursor(cs, HAUTEUR - 26);

                // Identité en haut, sur blanc
                c.texteCouleur(s.gras(), 13, X, nonVide(carte.nom()), s.primaire());
                c.avancer(16);
                if (rempli(carte.poste())) {
                    c.texteCouleur(s.normal(), 8.5f, X, carte.poste(), s.accent());
                    c.avancer(12);
                }
                if (rempli(carte.entreprise())) {
                    c.texteCouleur(s.normal(), 8.5f, X, carte.entreprise(), s.texte());
                }

                // Bande colorée en pied, coordonnées en blanc
                c.bandeau(0, 0, LARGEUR, BANDE_H, fond);
                float y = BANDE_H - 16;
                for (String champ : new String[]{carte.telephone(), carte.email(), carte.siteWeb(), carte.adresse()}) {
                    if (rempli(champ)) {
                        c.texteCouleurA(s.normal(), 8, X, y, champ, CLAIR);
                        y -= 11.5f;
                    }
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Échec du rendu de la carte de visite", e);
        }
    }
}
