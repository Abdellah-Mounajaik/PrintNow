package com.printnow.module.studio.service.gabarit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.printnow.module.studio.enums.TypeSupport;
import com.printnow.module.studio.model.ContenuCv;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.printnow.module.order.service.pdf.PdfFactureHelpers.Cursor;
import static com.printnow.module.order.service.pdf.PdfFactureHelpers.MARGE;
import static com.printnow.module.order.service.pdf.PdfFactureHelpers.nonVide;
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.envelopper;
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.joindre;
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.rempli;
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.tailleQuiTient;
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.vide;

/**
 * Maquette « minimaliste » : beaucoup de blanc, grand nom, fonction en majuscules
 * espacées, sections à marqueur carré et titres espacés, compétences/langues en
 * chips. Allure éditoriale moderne, une seule colonne.
 */
@Component
@RequiredArgsConstructor
public class CvMinimalGabarit implements Gabarit {

    public static final String CODE = "cv-minimal";

    private static final float LARGEUR_PAGE = PDRectangle.A4.getWidth();
    private static final float HAUTEUR_PAGE = PDRectangle.A4.getHeight();
    private static final float LARGEUR_UTILE = LARGEUR_PAGE - 2 * MARGE;

    private final ObjectMapper mapper;

    @Override
    public TypeSupport type() {
        return TypeSupport.CV;
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public String promptSysteme() {
        return ContenuCv.PROMPT;
    }

    @Override
    public byte[] rendre(String json, Style style) throws Exception {
        ContenuCv cv = mapper.readValue(json, ContenuCv.class);
        if (cv == null || (cv.nom() == null && cv.titrePro() == null
                && (cv.experiences() == null || cv.experiences().isEmpty()))) {
            throw new IllegalArgumentException("JSON du CV vide ou inexploitable");
        }
        return dessiner(cv, style);
    }

    private byte[] dessiner(ContenuCv cv, Style s) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                Cursor c = new Cursor(cs, HAUTEUR_PAGE - 76);
                c.bandeau(0, 0, LARGEUR_PAGE, HAUTEUR_PAGE, s.page());   // fond de page

                // Nom
                String nom = nonVide(cv.nom());
                float tNom = tailleQuiTient(s.gras(), nom, 32, 20, LARGEUR_UTILE);
                c.texteCouleur(s.gras(), tNom, MARGE, nom, s.titre());
                c.avancer(tNom + 2);
                if (rempli(cv.titrePro())) {
                    Formes.texteEspace(cs, s.normal(), 12, MARGE, c.y, cv.titrePro().toUpperCase(), 2f, s.accentPage());
                    c.avancer(20);
                }
                // Filet + contact
                c.avancer(2);
                c.bandeau(MARGE, c.y, LARGEUR_UTILE, 1.2f, s.filet());
                c.avancer(16);
                String contact = ligneContact(cv.contact());
                if (!contact.isBlank()) {
                    c.texteCouleur(s.normal(), 10, MARGE, contact, s.texteDoux());
                    c.avancer(20);
                }
                c.avancer(6);

                if (!vide(cv.competences())) {
                    label(cs, c, "Compétences", s);
                    c.y = chips(cs, c, s, cv.competences(), c.y);
                    c.avancer(10);
                }
                experiences(cs, c, cv.experiences(), s);
                formations(cs, c, cv.formations(), s);
                if (!vide(cv.langues())) {
                    label(cs, c, "Langues", s);
                    c.y = chips(cs, c, s, cv.langues(), c.y);
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Échec du rendu du CV", e);
        }
    }

    private void experiences(PDPageContentStream cs, Cursor c, List<ContenuCv.Experience> experiences, Style s) throws IOException {
        if (vide(experiences)) return;
        label(cs, c, "Expériences", s);
        for (ContenuCv.Experience e : experiences) {
            String titre = joindre(" — ", e.poste(), e.entreprise());
            if (!titre.isBlank()) {
                for (String ligne : envelopper(s.gras(), 11.5f, titre, LARGEUR_UTILE)) {
                    c.texteCouleur(s.gras(), 11.5f, MARGE, ligne, s.encre());
                    c.avancer(15);
                }
            }
            if (rempli(e.periode())) {
                c.texteCouleur(s.normal(), 9, MARGE, e.periode(), s.accentPage());
                c.avancer(13);
            }
            if (rempli(e.description())) {
                for (String ligne : envelopper(s.normal(), 10, e.description(), LARGEUR_UTILE)) {
                    c.texteCouleur(s.normal(), 10, MARGE, ligne, s.encre());
                    c.avancer(13);
                }
            }
            c.avancer(11);
        }
        c.avancer(4);
    }

    private void formations(PDPageContentStream cs, Cursor c, List<ContenuCv.Formation> formations, Style s) throws IOException {
        if (vide(formations)) return;
        label(cs, c, "Formations", s);
        for (ContenuCv.Formation f : formations) {
            String ligne = joindre(" — ", f.diplome(), f.ecole());
            if (rempli(f.annee())) ligne = joindre("  ·  ", ligne, f.annee());
            if (!ligne.isBlank()) {
                for (String morceau : envelopper(s.normal(), 10.5f, ligne, LARGEUR_UTILE)) {
                    c.texteCouleur(s.normal(), 10.5f, MARGE, morceau, s.encre());
                    c.avancer(15);
                }
            }
        }
        c.avancer(12);
    }

    private void label(PDPageContentStream cs, Cursor c, String titre, Style s) throws IOException {
        c.bandeau(MARGE, c.y - 1, 8, 8, s.accentPage());
        Formes.texteEspace(cs, s.gras(), 12.5f, MARGE + 16, c.y, titre.toUpperCase(), 1.6f, s.titre());
        c.avancer(20);
    }

    /** Rangée(s) de chips repliées sur la largeur utile. */
    private float chips(PDPageContentStream cs, Cursor c, Style s, List<String> items, float y) throws IOException {
        int[] bg = s.chip();
        int[] txt = s.chipTexte();
        float x0 = MARGE, px = x0, py = y, h = 19, gap = 7, padX = 11;
        for (String item : items) {
            float w = s.normal().getStringWidth(item) / 1000f * 9.5f + 2 * padX;
            if (px + w > x0 + LARGEUR_UTILE) {
                px = x0;
                py -= h + gap;
            }
            Formes.rectArrondi(cs, px, py - h, w, h, 5, bg);
            c.texteCouleurA(s.normal(), 9.5f, px + padX, py - h + 6, item, txt);
            px += w + gap;
        }
        return py - h - 8;
    }

    private String ligneContact(ContenuCv.Contact contact) {
        if (contact == null) return "";
        List<String> morceaux = new ArrayList<>();
        if (rempli(contact.email())) morceaux.add(contact.email());
        if (rempli(contact.telephone())) morceaux.add(contact.telephone());
        if (rempli(contact.ville())) morceaux.add(contact.ville());
        return String.join("   •   ", morceaux);
    }
}
