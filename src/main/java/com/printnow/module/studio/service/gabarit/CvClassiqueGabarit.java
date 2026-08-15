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
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.vide;

/**
 * Maquette « classique » : une seule colonne, sobre. En-tête (nom, titre, contact)
 * séparé du corps par un filet, puis les sections empilées avec des titres colorés.
 * La plus proche d'un CV traditionnel.
 */
@Component
@RequiredArgsConstructor
public class CvClassiqueGabarit implements Gabarit {

    public static final String CODE = "cv-classique";

    private static final float LARGEUR_UTILE = PDRectangle.A4.getWidth() - 2 * MARGE;

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
                Cursor c = new Cursor(cs, PDRectangle.A4.getHeight() - MARGE);

                c.texteCouleur(s.gras(), 24, MARGE, nonVide(cv.nom()), s.primaire());
                c.avancer(26);
                if (rempli(cv.titrePro())) {
                    c.texteCouleur(s.normal(), 13, MARGE, cv.titrePro(), s.accent());
                    c.avancer(18);
                }
                String contact = ligneContact(cv.contact());
                if (!contact.isBlank()) {
                    c.texteCouleur(s.normal(), 10, MARGE, contact, s.texte());
                    c.avancer(16);
                }
                c.avancer(6);
                c.ligneHorizontale(MARGE, MARGE + LARGEUR_UTILE);
                c.avancer(22);

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
            c.texte(s.normal(), 10, MARGE, ligne);
            c.avancer(14);
        }
        c.avancer(10);
    }

    private void experiences(Cursor c, List<ContenuCv.Experience> experiences, Style s) throws IOException {
        if (vide(experiences)) return;
        titreSection(c, "Expériences", s);
        for (ContenuCv.Experience e : experiences) {
            String titre = joindre(" — ", e.poste(), e.entreprise());
            if (!titre.isBlank()) {
                c.texte(s.gras(), 11, MARGE, titre);
                c.avancer(14);
            }
            if (rempli(e.periode())) {
                c.texteCouleur(s.normal(), 9, MARGE, e.periode(), s.texte());
                c.avancer(12);
            }
            if (rempli(e.description())) {
                for (String ligne : envelopper(s.normal(), 10, e.description(), LARGEUR_UTILE)) {
                    c.texte(s.normal(), 10, MARGE, ligne);
                    c.avancer(13);
                }
            }
            c.avancer(8);
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
                c.texte(s.normal(), 10, MARGE, ligne);
                c.avancer(14);
            }
        }
        c.avancer(10);
    }

    private void langues(Cursor c, List<String> langues, Style s) throws IOException {
        if (vide(langues)) return;
        titreSection(c, "Langues", s);
        c.texte(s.normal(), 10, MARGE, String.join("   •   ", langues));
        c.avancer(14);
    }

    private void titreSection(Cursor c, String titre, Style s) throws IOException {
        c.texteCouleur(s.gras(), 13, MARGE, titre.toUpperCase(), s.primaire());
        c.avancer(18);
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
