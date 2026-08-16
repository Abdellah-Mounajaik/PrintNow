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
 * Maquette « monogramme » (moderne, claire) : pastille ronde avec les initiales,
 * nom et fonction en micro-label espacé à côté, coordonnées à puces, anneau
 * d'accent en coin. Beaucoup de blanc.
 */
@Component
@RequiredArgsConstructor
public class CarteMonogrammeGabarit implements Gabarit {

    public static final String CODE = "carte-monogramme";

    private static final float MM = 72f / 25.4f;
    private static final float LARGEUR = 85 * MM;
    private static final float HAUTEUR = 55 * MM;

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

            int[] accent = s.accentPage();

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                Cursor c = new Cursor(cs, HAUTEUR);
                c.bandeau(0, 0, LARGEUR, HAUTEUR, s.page());   // fond de page

                // Pastille de monogramme
                float bx = 42, by = HAUTEUR - 40, br = 22;
                boolean plein = Formes.badge(cs, s.variante().badge(), bx, by, br, s.pastille());
                String ini = Formes.initiales(nonVide(carte.nom()));
                if (!ini.isBlank()) {
                    int[] col = plein ? s.surPastille() : s.titre();
                    float wIni = s.gras().getStringWidth(ini) / 1000f * 18f;
                    c.texteCouleurA(s.gras(), 18, bx - wIni / 2f, by - 0.35f * 18f, ini, col);
                }

                // Nom + fonction
                float nx = 76;
                String nom = nonVide(carte.nom());
                float tNom = tailleQuiTient(s.gras(), nom, 16, 11, LARGEUR - 20 - nx);
                c.texteCouleurA(s.gras(), tNom, nx, HAUTEUR - 45, nom, s.titre());
                if (rempli(carte.poste())) {
                    Formes.texteEspace(cs, s.normal(), 7, nx, HAUTEUR - 61, carte.poste().toUpperCase(), 1.4f, accent);
                }

                // Filet séparateur
                c.bandeau(20, HAUTEUR - 80, LARGEUR - 40, 0.8f, s.filet());

                // Coordonnées à puces
                float y = HAUTEUR - 98;
                if (rempli(carte.entreprise())) {
                    c.texteCouleurA(s.gras(), 8.5f, 22, y, carte.entreprise(), s.titre());
                    y -= 14;
                }
                for (String champ : new String[]{carte.telephone(), carte.email(), carte.siteWeb(), carte.adresse()}) {
                    if (rempli(champ)) {
                        Formes.marqueur(cs, s.variante().puce(), 24, y + 2.6f, 1.7f, accent);
                        c.texteCouleurA(s.normal(), 8, 33, y, champ, s.texteDoux());
                        y -= 13;
                    }
                }

                // Anneau d'accent en coin bas-droit
                Formes.anneau(cs, LARGEUR - 2, 6, 18, 2.2f, accent);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Échec du rendu de la carte de visite", e);
        }
    }
}
