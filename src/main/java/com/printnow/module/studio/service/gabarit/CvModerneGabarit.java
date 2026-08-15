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
import static com.printnow.module.order.service.pdf.PdfFactureHelpers.nonVide;
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.eclaircir;
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.envelopper;
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.foncer;
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.joindre;
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.motLePlusLong;
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.rempli;
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.tailleQuiTient;
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.vide;

/**
 * Maquette « moderne » à deux colonnes : une colonne latérale colorée (nom,
 * titre, contact, compétences, langues, en blanc sur l'aplat) et une colonne
 * principale sur fond blanc (expériences, formations).
 */
@Component
@RequiredArgsConstructor
public class CvModerneGabarit implements Gabarit {

    public static final String CODE = "cv-moderne";

    private static final float LARGEUR_PAGE = PDRectangle.A4.getWidth();   // 595
    private static final float HAUTEUR_PAGE = PDRectangle.A4.getHeight();  // 842

    private static final float SIDEBAR_W = 200f;
    private static final float PAD = 24f;
    private static final float COL_G_X = PAD;                       // texte de la colonne latérale
    private static final float COL_G_W = SIDEBAR_W - 2 * PAD;       // 152
    private static final float COL_D_X = SIDEBAR_W + 32f;           // 232
    private static final float COL_D_W = LARGEUR_PAGE - 42f - COL_D_X;
    private static final float HAUT = HAUTEUR_PAGE - 56f;           // y de départ des deux colonnes

    private static final int[] BLANC = {255, 255, 255};
    private static final int[] CLAIR = {214, 219, 229};             // texte discret sur l'aplat
    private static final int[] ENCRE = {40, 42, 48};               // texte de la colonne principale

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

            int[] fond = foncer(s.primaire(), 72);        // aplat sombre : le texte blanc doit ressortir
            int[] surTitre = eclaircir(s.accent(), 200);  // accent éclairci pour les titres sur l'aplat

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                Cursor g = new Cursor(cs, HAUT);
                Cursor d = new Cursor(cs, HAUT);

                g.bandeau(0, 0, SIDEBAR_W, HAUTEUR_PAGE, fond);   // fond de la colonne, avant tout texte
                colonneLaterale(g, cv, s, surTitre);
                colonnePrincipale(d, cv, s);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Échec du rendu du CV", e);
        }
    }

    // --- colonne latérale colorée ---------------------------------------------

    private void colonneLaterale(Cursor g, ContenuCv cv, Style s, int[] surTitre) throws IOException {
        String nom = nonVide(cv.nom());
        float tNom = tailleQuiTient(s.gras(), motLePlusLong(nom), 21, 13, COL_G_W);
        for (String ligne : envelopper(s.gras(), tNom, nom, COL_G_W)) {
            g.texteCouleur(s.gras(), tNom, COL_G_X, ligne, BLANC);
            g.avancer(tNom + 3);
        }
        if (rempli(cv.titrePro())) {
            g.avancer(3);
            for (String ligne : envelopper(s.normal(), 11, cv.titrePro(), COL_G_W)) {
                g.texteCouleur(s.normal(), 11, COL_G_X, ligne, surTitre);
                g.avancer(14);
            }
        }
        g.avancer(20);

        List<String> contact = contactLignes(cv.contact());
        if (!contact.isEmpty()) {
            titreLaterale(g, "Contact", s, surTitre);
            for (String ligne : contact) {
                float t = tailleQuiTient(s.normal(), ligne, 9.5f, 7f, COL_G_W);
                g.texteCouleur(s.normal(), t, COL_G_X, ligne, CLAIR);
                g.avancer(13);
            }
            g.avancer(12);
        }

        if (!vide(cv.competences())) {
            titreLaterale(g, "Compétences", s, surTitre);
            for (String comp : cv.competences()) {
                elementListe(g, comp, s);
            }
            g.avancer(12);
        }

        if (!vide(cv.langues())) {
            titreLaterale(g, "Langues", s, surTitre);
            for (String langue : cv.langues()) {
                elementListe(g, langue, s);
            }
        }
    }

    private void titreLaterale(Cursor g, String titre, Style s, int[] couleur) throws IOException {
        g.texteCouleur(s.gras(), 10.5f, COL_G_X, titre.toUpperCase(), couleur);
        g.avancer(7);
        g.bandeau(COL_G_X, g.y, 26, 2f, couleur);
        g.avancer(13);
    }

    private void elementListe(Cursor g, String texte, Style s) throws IOException {
        List<String> lignes = envelopper(s.normal(), 9.5f, texte, COL_G_W - 10);
        boolean premier = true;
        for (String ligne : lignes) {
            g.texteCouleur(s.normal(), 9.5f, COL_G_X, (premier ? "•  " : "    ") + ligne, CLAIR);
            g.avancer(13);
            premier = false;
        }
    }

    // --- colonne principale ----------------------------------------------------

    private void colonnePrincipale(Cursor d, ContenuCv cv, Style s) throws IOException {
        experiences(d, cv.experiences(), s);
        formations(d, cv.formations(), s);
    }

    private void experiences(Cursor d, List<ContenuCv.Experience> experiences, Style s) throws IOException {
        if (vide(experiences)) return;
        titrePrincipal(d, "Expériences", s);
        for (ContenuCv.Experience e : experiences) {
            String titre = joindre(" — ", e.poste(), e.entreprise());
            if (!titre.isBlank()) {
                for (String ligne : envelopper(s.gras(), 11, titre, COL_D_W)) {
                    d.texteCouleur(s.gras(), 11, COL_D_X, ligne, ENCRE);
                    d.avancer(14);
                }
            }
            if (rempli(e.periode())) {
                d.texteCouleur(s.normal(), 9, COL_D_X, e.periode(), s.texte());
                d.avancer(13);
            }
            if (rempli(e.description())) {
                for (String ligne : envelopper(s.normal(), 10, e.description(), COL_D_W)) {
                    d.texteCouleur(s.normal(), 10, COL_D_X, ligne, ENCRE);
                    d.avancer(13);
                }
            }
            d.avancer(9);
        }
        d.avancer(4);
    }

    private void formations(Cursor d, List<ContenuCv.Formation> formations, Style s) throws IOException {
        if (vide(formations)) return;
        titrePrincipal(d, "Formations", s);
        for (ContenuCv.Formation f : formations) {
            String titre = joindre(" — ", f.diplome(), f.ecole());
            if (!titre.isBlank()) {
                for (String ligne : envelopper(s.gras(), 10.5f, titre, COL_D_W)) {
                    d.texteCouleur(s.gras(), 10.5f, COL_D_X, ligne, ENCRE);
                    d.avancer(13);
                }
            }
            if (rempli(f.annee())) {
                d.texteCouleur(s.normal(), 9, COL_D_X, f.annee(), s.texte());
                d.avancer(13);
            }
            d.avancer(7);
        }
    }

    private void titrePrincipal(Cursor d, String titre, Style s) throws IOException {
        d.texteCouleur(s.gras(), 13, COL_D_X, titre.toUpperCase(), s.primaire());
        d.avancer(8);
        d.bandeau(COL_D_X, d.y, 34, 2.5f, s.accent());
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
