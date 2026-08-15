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

/**
 * Dessine un flyer A5 dans le style demandé : bandeau de titre coloré, accroche,
 * blocs d'information centrés, contact en pied.
 */
@Component
@RequiredArgsConstructor
public class FlyerGabarit implements Gabarit {

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
        return """
            Tu rédiges le contenu d'un flyer promotionnel, en français, à partir de
            la description fournie.

            Réponds UNIQUEMENT par un objet JSON valide, sans texte autour, sans
            balises Markdown, exactement à ce format :
            {
              "titre": "",
              "accroche": "",
              "blocs": [ { "libelle": "", "valeur": "" } ],
              "contact": { "telephone": "", "adresse": "", "email": "" }
            }

            Règles :
            - N'invente aucun fait : n'utilise que ce que la description contient.
            - "titre" est court et accrocheur ; "accroche" est une phrase percutante.
            - "blocs" liste les informations clés (ex : « Menu du midi » / « 15 € »).
            - Laisse une chaîne vide quand l'information manque.
            """;
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

    /** Plus grande taille (entre min et préférée) à laquelle le texte tient dans la largeur. */
    private float tailleQuiTient(PDType1Font font, String texte, float taillePref, float tailleMin, float largeurMax) throws IOException {
        float taille = taillePref;
        while (taille > tailleMin && font.getStringWidth(texte) / 1000f * taille > largeurMax) {
            taille -= 1;
        }
        return taille;
    }

    /** Coupe un texte en lignes qui tiennent dans la largeur donnée. */
    private List<String> envelopper(PDType1Font font, float taille, String texte, float largeurMax) throws IOException {
        List<String> lignes = new ArrayList<>();
        if (!rempli(texte)) return lignes;
        StringBuilder courante = new StringBuilder();
        for (String mot : texte.split("\\s+")) {
            String essai = courante.isEmpty() ? mot : courante + " " + mot;
            if (font.getStringWidth(essai) / 1000f * taille > largeurMax && courante.length() > 0) {
                lignes.add(courante.toString());
                courante = new StringBuilder(mot);
            } else {
                courante = new StringBuilder(essai);
            }
        }
        if (courante.length() > 0) lignes.add(courante.toString());
        return lignes;
    }

    private boolean rempli(String s) {
        return s != null && !s.isBlank();
    }
}
