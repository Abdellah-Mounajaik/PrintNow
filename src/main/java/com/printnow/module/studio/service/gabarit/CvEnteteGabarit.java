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
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.eclaircir;
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.envelopper;
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.foncer;
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.joindre;
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.rempli;
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.tailleQuiTient;
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.vide;

/**
 * Maquette « en-tête » : un grand bandeau coloré en haut porte le nom, le titre
 * et le contact en blanc ; le corps suit sur une seule colonne, sections empilées
 * avec titres soulignés d'un trait d'accent.
 */
@Component
@RequiredArgsConstructor
public class CvEnteteGabarit implements Gabarit {

    public static final String CODE = "cv-entete";

    private static final float LARGEUR_PAGE = PDRectangle.A4.getWidth();   // 595
    private static final float HAUTEUR_PAGE = PDRectangle.A4.getHeight();  // 842
    private static final float LARGEUR_UTILE = LARGEUR_PAGE - 2 * MARGE;
    private static final float BANDEAU_H = 132f;

    private static final int[] BLANC = {255, 255, 255};
    private static final int[] CLAIR = {222, 226, 234};
    private static final int[] ENCRE = {40, 42, 48};

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

            int[] fond = foncer(s.primaire(), 74);
            int[] surTitre = eclaircir(s.accent(), 205);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                Cursor c = new Cursor(cs, HAUTEUR_PAGE - MARGE);

                // Bandeau d'en-tête, pleine largeur.
                c.bandeau(0, HAUTEUR_PAGE - BANDEAU_H, LARGEUR_PAGE, BANDEAU_H, fond);

                String nom = nonVide(cv.nom());
                float tNom = tailleQuiTient(s.gras(), nom, 27, 17, LARGEUR_UTILE);
                c.texteCouleurA(s.gras(), tNom, MARGE, HAUTEUR_PAGE - 54, nom, BLANC);
                if (rempli(cv.titrePro())) {
                    c.texteCouleurA(s.normal(), 13, MARGE, HAUTEUR_PAGE - 78, cv.titrePro(), surTitre);
                }
                String contact = ligneContact(cv.contact());
                if (!contact.isBlank()) {
                    c.texteCouleurA(s.normal(), 10, MARGE, HAUTEUR_PAGE - 100, contact, CLAIR);
                }

                // Corps, une colonne.
                c.y = HAUTEUR_PAGE - BANDEAU_H - 34;
                competences(c, cv.competences(), s);
                experiences(c, cv.experiences(), s);
                formations(c, cv.formations(), s);
                langues(c, cv.langues(), s);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Échec du rendu du CV", e);
        }
    }

    private void competences(Cursor c, List<String> competences, Style s) throws IOException {
        if (vide(competences)) return;
        titreSection(c, "Compétences", s);
        for (String ligne : envelopper(s.normal(), 10, String.join("   •   ", competences), LARGEUR_UTILE)) {
            c.texteCouleur(s.normal(), 10, MARGE, ligne, ENCRE);
            c.avancer(14);
        }
        c.avancer(12);
    }

    private void experiences(Cursor c, List<ContenuCv.Experience> experiences, Style s) throws IOException {
        if (vide(experiences)) return;
        titreSection(c, "Expériences", s);
        for (ContenuCv.Experience e : experiences) {
            String titre = joindre(" — ", e.poste(), e.entreprise());
            if (!titre.isBlank()) {
                for (String ligne : envelopper(s.gras(), 11, titre, LARGEUR_UTILE)) {
                    c.texteCouleur(s.gras(), 11, MARGE, ligne, ENCRE);
                    c.avancer(14);
                }
            }
            if (rempli(e.periode())) {
                c.texteCouleur(s.normal(), 9, MARGE, e.periode(), s.texte());
                c.avancer(12);
            }
            if (rempli(e.description())) {
                for (String ligne : envelopper(s.normal(), 10, e.description(), LARGEUR_UTILE)) {
                    c.texteCouleur(s.normal(), 10, MARGE, ligne, ENCRE);
                    c.avancer(13);
                }
            }
            c.avancer(9);
        }
        c.avancer(4);
    }

    private void formations(Cursor c, List<ContenuCv.Formation> formations, Style s) throws IOException {
        if (vide(formations)) return;
        titreSection(c, "Formations", s);
        for (ContenuCv.Formation f : formations) {
            String ligne = joindre(" — ", f.diplome(), f.ecole());
            if (rempli(f.annee())) ligne = joindre("  ·  ", ligne, f.annee());
            if (!ligne.isBlank()) {
                for (String morceau : envelopper(s.normal(), 10, ligne, LARGEUR_UTILE)) {
                    c.texteCouleur(s.normal(), 10, MARGE, morceau, ENCRE);
                    c.avancer(14);
                }
            }
        }
        c.avancer(12);
    }

    private void langues(Cursor c, List<String> langues, Style s) throws IOException {
        if (vide(langues)) return;
        titreSection(c, "Langues", s);
        c.texteCouleur(s.normal(), 10, MARGE, String.join("   •   ", langues), ENCRE);
        c.avancer(14);
    }

    private void titreSection(Cursor c, String titre, Style s) throws IOException {
        c.texteCouleur(s.gras(), 13, MARGE, titre.toUpperCase(), s.primaire());
        c.avancer(8);
        c.bandeau(MARGE, c.y, 34, 2.5f, s.accent());
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
