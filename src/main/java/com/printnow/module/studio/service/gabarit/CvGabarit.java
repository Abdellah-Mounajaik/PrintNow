package com.printnow.module.studio.service.gabarit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.printnow.module.studio.enums.TypeSupport;
import com.printnow.module.studio.model.ContenuCv;
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
import static com.printnow.module.order.service.pdf.PdfFactureHelpers.MARGE;
import static com.printnow.module.order.service.pdf.PdfFactureHelpers.nonVide;

/**
 * Dessine un CV à partir de son contenu structuré, dans le style demandé
 * (palette + police). Le contenu ne fait que remplir des emplacements fixes ;
 * ce sont le style et le gabarit qui font l'allure.
 */
@Component
@RequiredArgsConstructor
public class CvGabarit implements Gabarit {

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
        return """
            Tu es un assistant qui met en forme des CV. À partir de la description
            fournie par l'utilisateur, produis un CV structuré, en français.

            Réponds UNIQUEMENT par un objet JSON valide, sans texte autour, sans
            balises Markdown, exactement à ce format :
            {
              "nom": "",
              "titrePro": "",
              "contact": { "email": "", "telephone": "", "ville": "" },
              "competences": [],
              "experiences": [ { "poste": "", "entreprise": "", "periode": "", "description": "" } ],
              "formations": [ { "diplome": "", "ecole": "", "annee": "" } ],
              "langues": []
            }

            Règles :
            - N'invente aucun fait : n'utilise que ce que la description contient.
            - Améliore la formulation (style professionnel), sans mentir.
            - Laisse une chaîne vide ou une liste vide quand l'information manque.
            - "titrePro" est un intitulé court (ex : « Développeur Full-Stack »).
            """;
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
        for (String ligne : envelopper(s.normal(), 10, String.join("   •   ", competences))) {
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
                for (String ligne : envelopper(s.normal(), 10, e.description())) {
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

    /** Coupe un texte en lignes qui tiennent dans la largeur utile. */
    private List<String> envelopper(PDType1Font font, float taille, String texte) throws IOException {
        List<String> lignes = new ArrayList<>();
        if (!rempli(texte)) return lignes;
        StringBuilder courante = new StringBuilder();
        for (String mot : texte.split("\\s+")) {
            String essai = courante.isEmpty() ? mot : courante + " " + mot;
            if (font.getStringWidth(essai) / 1000f * taille > LARGEUR_UTILE && !courante.isEmpty()) {
                lignes.add(courante.toString());
                courante = new StringBuilder(mot);
            } else {
                courante = new StringBuilder(essai);
            }
        }
        if (!courante.isEmpty()) lignes.add(courante.toString());
        return lignes;
    }

    private String joindre(String sep, String a, String b) {
        boolean ra = rempli(a), rb = rempli(b);
        if (ra && rb) return a + sep + b;
        return ra ? a : (rb ? b : "");
    }

    private boolean rempli(String s) {
        return s != null && !s.isBlank();
    }

    private boolean vide(List<?> liste) {
        return liste == null || liste.isEmpty();
    }
}
