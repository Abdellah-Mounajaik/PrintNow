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
 * Maquette « en-tête » modernisée : grand bandeau coloré avec monogramme, nom en
 * blanc et micro-label espacé, motif d'anneau ; corps sur une colonne, sections
 * en majuscules espacées, compétences et langues en chips.
 */
@Component
@RequiredArgsConstructor
public class CvEnteteGabarit implements Gabarit {

    public static final String CODE = "cv-entete";

    private static final float LARGEUR_PAGE = PDRectangle.A4.getWidth();
    private static final float HAUTEUR_PAGE = PDRectangle.A4.getHeight();
    private static final float LARGEUR_UTILE = LARGEUR_PAGE - 2 * MARGE;
    private static final float BANDEAU_H = 150f;

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

            int[] accent = s.accentPanneau();

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                Cursor c = new Cursor(cs, HAUTEUR_PAGE - MARGE);

                c.bandeau(0, 0, LARGEUR_PAGE, HAUTEUR_PAGE, s.page());                          // fond de page
                c.bandeau(0, HAUTEUR_PAGE - BANDEAU_H, LARGEUR_PAGE, BANDEAU_H, s.panneau());    // bandeau
                Formes.anneau(cs, LARGEUR_PAGE - 14, HAUTEUR_PAGE - 14, 26, 3f, accent);

                // Monogramme
                float mcx = MARGE + 30, mcy = HAUTEUR_PAGE - 64, mr = 28;
                Formes.anneau(cs, mcx, mcy, mr, 2.4f, accent);
                String ini = Formes.initiales(nonVide(cv.nom()));
                if (!ini.isBlank()) {
                    float w = s.gras().getStringWidth(ini) / 1000f * 20f;
                    c.texteCouleurA(s.gras(), 20, mcx - w / 2f, mcy - 0.34f * 20f, ini, s.surPanneau());
                }

                // Nom + fonction + contact
                float tx = MARGE + 74;
                String nom = nonVide(cv.nom());
                float tNom = tailleQuiTient(s.gras(), nom, 27, 17, LARGEUR_PAGE - MARGE - tx);
                c.texteCouleurA(s.gras(), tNom, tx, HAUTEUR_PAGE - 56, nom, s.surPanneau());
                if (rempli(cv.titrePro())) {
                    Formes.texteEspace(cs, s.normal(), 10.5f, tx, HAUTEUR_PAGE - 78, cv.titrePro().toUpperCase(), 1.6f, accent);
                }
                String contact = ligneContact(cv.contact());
                if (!contact.isBlank()) {
                    c.texteCouleurA(s.normal(), 9.5f, tx, HAUTEUR_PAGE - 100, contact, s.surPanneauDoux());
                }

                // Corps
                c.y = HAUTEUR_PAGE - BANDEAU_H - 34;
                if (!vide(cv.competences())) {
                    titreSection(cs, c, "Compétences", s);
                    c.y = chips(cs, c, s, cv.competences(), c.y);
                    c.avancer(8);
                }
                experiences(cs, c, cv.experiences(), s);
                formations(cs, c, cv.formations(), s);
                if (!vide(cv.langues())) {
                    titreSection(cs, c, "Langues", s);
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

    /** Rangée(s) de chips (compétences, langues), repliées sur la largeur utile. */
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

    private void experiences(PDPageContentStream cs, Cursor c, List<ContenuCv.Experience> experiences, Style s) throws IOException {
        if (vide(experiences)) return;
        titreSection(cs, c, "Expériences", s);
        for (ContenuCv.Experience e : experiences) {
            String titre = joindre(" — ", e.poste(), e.entreprise());
            if (!titre.isBlank()) {
                Formes.disque(cs, MARGE + 2, c.y + 3.2f, 2f, s.accentPage());
                for (String ligne : envelopper(s.gras(), 11, titre, LARGEUR_UTILE - 12)) {
                    c.texteCouleurA(s.gras(), 11, MARGE + 12, c.y, ligne, s.encre());
                    c.avancer(14);
                }
            }
            if (rempli(e.periode())) {
                c.texteCouleurA(s.normal(), 9, MARGE + 12, c.y, e.periode(), s.texteDoux());
                c.avancer(12);
            }
            if (rempli(e.description())) {
                for (String ligne : envelopper(s.normal(), 10, e.description(), LARGEUR_UTILE - 12)) {
                    c.texteCouleurA(s.normal(), 10, MARGE + 12, c.y, ligne, s.encre());
                    c.avancer(13);
                }
            }
            c.avancer(9);
        }
        c.avancer(4);
    }

    private void formations(PDPageContentStream cs, Cursor c, List<ContenuCv.Formation> formations, Style s) throws IOException {
        if (vide(formations)) return;
        titreSection(cs, c, "Formations", s);
        for (ContenuCv.Formation f : formations) {
            String ligne = joindre(" — ", f.diplome(), f.ecole());
            if (rempli(f.annee())) ligne = joindre("  ·  ", ligne, f.annee());
            if (!ligne.isBlank()) {
                Formes.disque(cs, MARGE + 2, c.y + 3.2f, 2f, s.accentPage());
                for (String morceau : envelopper(s.normal(), 10, ligne, LARGEUR_UTILE - 12)) {
                    c.texteCouleurA(s.normal(), 10, MARGE + 12, c.y, morceau, s.encre());
                    c.avancer(14);
                }
            }
        }
        c.avancer(12);
    }

    private void titreSection(PDPageContentStream cs, Cursor c, String titre, Style s) throws IOException {
        Formes.texteEspace(cs, s.gras(), 13, MARGE, c.y, titre.toUpperCase(), 1.3f, s.titre());
        c.avancer(8);
        c.bandeau(MARGE, c.y, 34, 2.5f, s.accentPage());
        c.avancer(16);
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
