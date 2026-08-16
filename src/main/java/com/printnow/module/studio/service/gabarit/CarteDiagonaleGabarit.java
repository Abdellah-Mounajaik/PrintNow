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
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.tailleQuiTient;

/**
 * Maquette « diagonale » (moderne) : bande colorée en pied avec un bord supérieur
 * incliné, identité en haut sur la page, coordonnées à puces sur la bande.
 */
@Component
@RequiredArgsConstructor
public class CarteDiagonaleGabarit implements Gabarit {

    public static final String CODE = "carte-diagonale";

    private static final float MM = 72f / 25.4f;
    private static final float LARGEUR = 85 * MM;
    private static final float HAUTEUR = 55 * MM;
    private static final float X = 18;

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

            int[] panneau = s.panneau();
            int[] accentBande = s.accentPanneau();
            float gauche = HAUTEUR * 0.40f;   // hauteur de bande côté gauche
            float droite = HAUTEUR * 0.50f;   // hauteur de bande côté droit (bord incliné)

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                Cursor c = new Cursor(cs, HAUTEUR);
                c.bandeau(0, 0, LARGEUR, HAUTEUR, s.page());   // fond de page

                // Bande colorée en pied, bord supérieur incliné
                Formes.polygone(cs, new float[]{0, LARGEUR, LARGEUR, 0}, new float[]{0, 0, droite, gauche}, panneau);
                Formes.polygone(cs, new float[]{0, LARGEUR, LARGEUR, 0},
                        new float[]{gauche + 3, droite + 3, droite + 6, gauche + 6}, s.accentPage());

                // Identité en haut, sur la page
                String nom = nonVide(carte.nom());
                float tNom = tailleQuiTient(s.gras(), nom, 17, 12, LARGEUR - 2 * X);
                c.texteCouleurA(s.gras(), tNom, X, HAUTEUR - 32, nom, s.titre());
                float yTop = HAUTEUR - 50;
                if (rempli(carte.poste())) {
                    Formes.texteEspace(cs, s.normal(), 7.5f, X, yTop, carte.poste().toUpperCase(), 1.5f, s.accentPage());
                    yTop -= 15;
                }
                if (rempli(carte.entreprise())) {
                    c.texteCouleurA(s.normal(), 9, X, yTop, carte.entreprise(), s.texteDoux());
                }

                // Coordonnées à puces, sur la bande
                float y = gauche - 14;
                for (String champ : new String[]{carte.telephone(), carte.email(), carte.siteWeb(), carte.adresse()}) {
                    if (rempli(champ)) {
                        Formes.disque(cs, X + 2, y + 2.6f, 1.6f, accentBande);
                        c.texteCouleurA(s.normal(), 8, X + 11, y, champ, s.surPanneau());
                        y -= 12.5f;
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
