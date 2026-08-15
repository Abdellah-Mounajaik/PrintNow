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
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.eclaircir;
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.foncer;
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.rempli;
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.tailleQuiTient;

/**
 * Maquette « pleine couleur » (moderne, sombre) : fond coloré plein, motif
 * d'anneau en coin, micro-label de fonction, gros nom en blanc, coordonnées à
 * puces claires.
 */
@Component
@RequiredArgsConstructor
public class CartePleineGabarit implements Gabarit {

    public static final String CODE = "carte-pleine";

    private static final float MM = 72f / 25.4f;
    private static final float LARGEUR = 85 * MM;
    private static final float HAUTEUR = 55 * MM;
    private static final float X = 20;

    private static final int[] BLANC = {255, 255, 255};
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
            int[] accentClair = eclaircir(s.accent(), 205);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                Cursor c = new Cursor(cs, HAUTEUR);
                c.bandeau(0, 0, LARGEUR, HAUTEUR, fond);

                // Motif géométrique en coin haut-droit
                Formes.anneau(cs, LARGEUR - 6, HAUTEUR - 6, 30, 3f, accentClair);
                Formes.disque(cs, LARGEUR - 30, HAUTEUR - 30, 3.5f, accentClair);

                // Micro-label de fonction
                if (rempli(carte.poste())) {
                    Formes.texteEspace(cs, s.normal(), 7, X, HAUTEUR - 46, carte.poste().toUpperCase(), 1.6f, accentClair);
                }

                // Nom
                String nom = nonVide(carte.nom());
                float tNom = tailleQuiTient(s.gras(), nom, 18, 12, LARGEUR - 2 * X);
                c.texteCouleurA(s.gras(), tNom, X, HAUTEUR - 66, nom, BLANC);

                // Trait d'accent
                c.bandeau(X, HAUTEUR - 76, 32, 2.5f, accentClair);

                // Entreprise + coordonnées à puces
                float y = HAUTEUR - 96;
                if (rempli(carte.entreprise())) {
                    c.texteCouleurA(s.gras(), 8.5f, X, y, carte.entreprise(), CLAIR);
                    y -= 15;
                }
                for (String champ : new String[]{carte.telephone(), carte.email(), carte.siteWeb(), carte.adresse()}) {
                    if (rempli(champ)) {
                        Formes.disque(cs, X + 2, y + 2.6f, 1.6f, accentClair);
                        c.texteCouleurA(s.normal(), 8, X + 11, y, champ, CLAIR);
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
