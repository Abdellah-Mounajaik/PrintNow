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
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.envelopper;
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.foncer;
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.motLePlusLong;
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.rempli;
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.tailleQuiTient;

/**
 * Maquette « deux tons » : panneau coloré à gauche (nom + fonction en blanc),
 * zone blanche à droite (entreprise et coordonnées).
 */
@Component
@RequiredArgsConstructor
public class CarteDuoGabarit implements Gabarit {

    public static final String CODE = "carte-duo";

    private static final float MM = 72f / 25.4f;
    private static final float LARGEUR = 85 * MM;
    private static final float HAUTEUR = 55 * MM;

    private static final float PANEL_W = LARGEUR * 0.42f;
    private static final float LG_X = 14f;
    private static final float LG_W = PANEL_W - 24f;
    private static final float RD_X = PANEL_W + 16f;
    private static final float RD_W = LARGEUR - 14f - RD_X;

    private static final int[] BLANC = {255, 255, 255};
    private static final int[] CLAIR = {222, 226, 234};

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
                Cursor g = new Cursor(cs, HAUTEUR - 56);
                Cursor d = new Cursor(cs, HAUTEUR - 40);

                g.bandeau(0, 0, PANEL_W, HAUTEUR, fond);   // panneau coloré à gauche

                // Panneau : nom + fonction
                String nom = nonVide(carte.nom());
                float tNom = tailleQuiTient(s.gras(), motLePlusLong(nom), 13, 8, LG_W);
                for (String ligne : envelopper(s.gras(), tNom, nom, LG_W)) {
                    g.texteCouleur(s.gras(), tNom, LG_X, ligne, BLANC);
                    g.avancer(tNom + 3);
                }
                if (rempli(carte.poste())) {
                    g.avancer(3);
                    for (String ligne : envelopper(s.normal(), 8, carte.poste(), LG_W)) {
                        g.texteCouleur(s.normal(), 8, LG_X, ligne, CLAIR);
                        g.avancer(11);
                    }
                }

                // Zone blanche : entreprise + coordonnées
                if (rempli(carte.entreprise())) {
                    for (String ligne : envelopper(s.gras(), 9.5f, carte.entreprise(), RD_W)) {
                        d.texteCouleur(s.gras(), 9.5f, RD_X, ligne, s.primaire());
                        d.avancer(14);
                    }
                    d.avancer(6);
                }
                for (String champ : new String[]{carte.telephone(), carte.email(), carte.siteWeb(), carte.adresse()}) {
                    if (rempli(champ)) {
                        for (String ligne : envelopper(s.normal(), 8, champ, RD_W)) {
                            d.texteCouleur(s.normal(), 8, RD_X, ligne, s.texte());
                            d.avancer(12);
                        }
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
