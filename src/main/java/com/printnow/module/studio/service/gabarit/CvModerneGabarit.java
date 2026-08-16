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
import static com.printnow.module.order.service.pdf.PdfFactureHelpers.nonVide;
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.envelopper;
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.joindre;
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.motLePlusLong;
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.rempli;
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.tailleQuiTient;
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.vide;

/**
 * Maquette « moderne » à colonne latérale : monogramme d'initiales et identité
 * centrés sur l'aplat, sections en majuscules espacées, listes à pastilles ;
 * expériences et formations dans la colonne principale.
 */
@Component
@RequiredArgsConstructor
public class CvModerneGabarit implements Gabarit {

    public static final String CODE = "cv-moderne";

    private static final float LARGEUR_PAGE = PDRectangle.A4.getWidth();
    private static final float HAUTEUR_PAGE = PDRectangle.A4.getHeight();

    private static final float SIDEBAR_W = 200f;
    private static final float PAD = 24f;
    private static final float COL_G_X = PAD;
    private static final float COL_G_W = SIDEBAR_W - 2 * PAD;
    private static final float COL_D_X = SIDEBAR_W + 32f;
    private static final float COL_D_W = LARGEUR_PAGE - 42f - COL_D_X;
    private static final float HAUT = HAUTEUR_PAGE - 56f;

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
                Cursor g = new Cursor(cs, HAUT);
                Cursor d = new Cursor(cs, HAUT);

                g.bandeau(0, 0, LARGEUR_PAGE, HAUTEUR_PAGE, s.page());     // fond de page
                g.bandeau(0, 0, SIDEBAR_W, HAUTEUR_PAGE, s.panneau());     // colonne latérale
                colonneLaterale(cs, g, cv, s);
                colonnePrincipale(cs, d, cv, s);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Échec du rendu du CV", e);
        }
    }

    // --- colonne latérale ------------------------------------------------------

    private void colonneLaterale(PDPageContentStream cs, Cursor g, ContenuCv cv, Style s) throws IOException {
        int[] accent = s.accentPanneau();
        // Monogramme : anneau + initiales, centré
        float mcx = SIDEBAR_W / 2f, mcy = HAUT + 4, mr = 24;
        Formes.anneau(cs, mcx, mcy, mr, 2.2f, accent);
        String ini = Formes.initiales(nonVide(cv.nom()));
        if (!ini.isBlank()) {
            float w = s.gras().getStringWidth(ini) / 1000f * 18f;
            g.texteCouleurA(s.gras(), 18, mcx - w / 2f, mcy - 0.34f * 18f, ini, s.surPanneau());
        }
        g.y = mcy - mr - 24;

        // Nom + fonction, centrés
        String nom = nonVide(cv.nom());
        float tNom = tailleQuiTient(s.gras(), motLePlusLong(nom), 18, 12, COL_G_W);
        for (String ligne : envelopper(s.gras(), tNom, nom, COL_G_W)) {
            centreG(g, s.gras(), tNom, ligne, s.surPanneau());
            g.avancer(tNom + 3);
        }
        if (rempli(cv.titrePro())) {
            g.avancer(2);
            for (String ligne : envelopper(s.normal(), 10, cv.titrePro(), COL_G_W)) {
                centreG(g, s.normal(), 10, ligne, accent);
                g.avancer(13);
            }
        }
        g.avancer(22);

        // Sections
        List<String> contact = contactLignes(cv.contact());
        if (!contact.isEmpty()) {
            labelLateral(cs, g, s, "Contact", accent);
            for (String ligne : contact) {
                float t = tailleQuiTient(s.normal(), ligne, 9.5f, 7f, COL_G_W - 10);
                Formes.disque(cs, COL_G_X + 2, g.y + 2.4f, 1.6f, accent);
                g.texteCouleurA(s.normal(), t, COL_G_X + 10, g.y, ligne, s.surPanneauDoux());
                g.avancer(13);
            }
            g.avancer(12);
        }
        if (!vide(cv.competences())) {
            labelLateral(cs, g, s, "Compétences", accent);
            for (String comp : cv.competences()) {
                pastilleItem(cs, g, s, comp, accent);
            }
            g.avancer(12);
        }
        if (!vide(cv.langues())) {
            labelLateral(cs, g, s, "Langues", accent);
            for (String langue : cv.langues()) {
                pastilleItem(cs, g, s, langue, accent);
            }
        }
    }

    private void centreG(Cursor g, PDType1Font font, float taille, String texte, int[] rgb) throws IOException {
        float w = font.getStringWidth(texte) / 1000f * taille;
        g.texteCouleurA(font, taille, (SIDEBAR_W - w) / 2f, g.y, texte, rgb);
    }

    private void labelLateral(PDPageContentStream cs, Cursor g, Style s, String label, int[] couleur) throws IOException {
        Formes.texteEspace(cs, s.gras(), 9.5f, COL_G_X, g.y, label.toUpperCase(), 1.3f, couleur);
        g.avancer(6);
        g.bandeau(COL_G_X, g.y, 22, 2f, couleur);
        g.avancer(13);
    }

    private void pastilleItem(PDPageContentStream cs, Cursor g, Style s, String texte, int[] couleurDot) throws IOException {
        List<String> lignes = envelopper(s.normal(), 9.5f, texte, COL_G_W - 10);
        boolean premier = true;
        for (String ligne : lignes) {
            if (premier) Formes.disque(cs, COL_G_X + 2, g.y + 2.6f, 1.6f, couleurDot);
            g.texteCouleurA(s.normal(), 9.5f, COL_G_X + 10, g.y, ligne, s.surPanneauDoux());
            g.avancer(13);
            premier = false;
        }
    }

    // --- colonne principale ----------------------------------------------------

    private void colonnePrincipale(PDPageContentStream cs, Cursor d, ContenuCv cv, Style s) throws IOException {
        experiences(cs, d, cv.experiences(), s);
        formations(cs, d, cv.formations(), s);
    }

    private void experiences(PDPageContentStream cs, Cursor d, List<ContenuCv.Experience> experiences, Style s) throws IOException {
        if (vide(experiences)) return;
        titrePrincipal(cs, d, "Expériences", s);
        for (ContenuCv.Experience e : experiences) {
            String titre = joindre(" — ", e.poste(), e.entreprise());
            if (!titre.isBlank()) {
                Formes.disque(cs, COL_D_X + 2, d.y + 3.2f, 2f, s.accentPage());
                for (String ligne : envelopper(s.gras(), 11, titre, COL_D_W - 12)) {
                    d.texteCouleurA(s.gras(), 11, COL_D_X + 12, d.y, ligne, s.encre());
                    d.avancer(14);
                }
            }
            if (rempli(e.periode())) {
                d.texteCouleurA(s.normal(), 9, COL_D_X + 12, d.y, e.periode(), s.texteDoux());
                d.avancer(13);
            }
            if (rempli(e.description())) {
                for (String ligne : envelopper(s.normal(), 10, e.description(), COL_D_W - 12)) {
                    d.texteCouleurA(s.normal(), 10, COL_D_X + 12, d.y, ligne, s.encre());
                    d.avancer(13);
                }
            }
            d.avancer(9);
        }
        d.avancer(4);
    }

    private void formations(PDPageContentStream cs, Cursor d, List<ContenuCv.Formation> formations, Style s) throws IOException {
        if (vide(formations)) return;
        titrePrincipal(cs, d, "Formations", s);
        for (ContenuCv.Formation f : formations) {
            String titre = joindre(" — ", f.diplome(), f.ecole());
            if (!titre.isBlank()) {
                Formes.disque(cs, COL_D_X + 2, d.y + 3.2f, 2f, s.accentPage());
                for (String ligne : envelopper(s.gras(), 10.5f, titre, COL_D_W - 12)) {
                    d.texteCouleurA(s.gras(), 10.5f, COL_D_X + 12, d.y, ligne, s.encre());
                    d.avancer(13);
                }
            }
            if (rempli(f.annee())) {
                d.texteCouleurA(s.normal(), 9, COL_D_X + 12, d.y, f.annee(), s.texteDoux());
                d.avancer(13);
            }
            d.avancer(7);
        }
    }

    private void titrePrincipal(PDPageContentStream cs, Cursor d, String titre, Style s) throws IOException {
        Formes.texteEspace(cs, s.gras(), 12.5f, COL_D_X, d.y, titre.toUpperCase(), 1.2f, s.titre());
        d.avancer(8);
        d.bandeau(COL_D_X, d.y, 30, 2.5f, s.accentPage());
        d.avancer(16);
    }

    private List<String> contactLignes(ContenuCv.Contact contact) {
        List<String> lignes = new ArrayList<>();
        if (contact == null) return lignes;
        if (rempli(contact.email())) lignes.add(contact.email());
        if (rempli(contact.telephone())) lignes.add(contact.telephone());
        if (rempli(contact.ville())) lignes.add(contact.ville());
        return lignes;
    }
}
