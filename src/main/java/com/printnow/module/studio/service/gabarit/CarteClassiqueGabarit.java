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
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.rempli;

/**
 * Maquette « classique » : barre d'accent à gauche, identité puis coordonnées
 * empilées sur fond blanc.
 */
@Component
@RequiredArgsConstructor
public class CarteClassiqueGabarit implements Gabarit {

    public static final String CODE = "carte-classique";

    /** 1 mm = 72/25.4 points. */
    private static final float MM = 72f / 25.4f;
    private static final float LARGEUR = 85 * MM;
    private static final float HAUTEUR = 55 * MM;
    private static final float X = 22;

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

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                Cursor c = new Cursor(cs, HAUTEUR - 24);

                c.bandeau(0, 0, 8, HAUTEUR, s.primaire());   // barre d'accent à gauche

                c.texteCouleur(s.gras(), 13, X, nonVide(carte.nom()), s.primaire());
                c.avancer(15);
                if (rempli(carte.poste())) {
                    c.texteCouleur(s.normal(), 8.5f, X, carte.poste(), s.accent());
                    c.avancer(11);
                }
                if (rempli(carte.entreprise())) {
                    c.texteCouleur(s.normal(), 8.5f, X, carte.entreprise(), s.texte());
                    c.avancer(11);
                }

                c.avancer(6);
                for (String ligne : new String[]{carte.telephone(), carte.email(), carte.siteWeb(), carte.adresse()}) {
                    if (rempli(ligne)) {
                        c.texteCouleur(s.normal(), 8, X, ligne, s.texte());
                        c.avancer(11);
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
