package com.printnow.module.studio.service.gabarit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.printnow.module.studio.enums.TypeSupport;
import com.printnow.module.studio.model.ContenuFlyer;
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
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.motLePlusLong;
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.rempli;
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.tailleQuiTient;

/**
 * Maquette « bande latérale » : une colonne colorée à gauche porte le titre et
 * l'accroche en blanc ; la zone blanche à droite reçoit les blocs d'information
 * et le contact.
 */
@Component
@RequiredArgsConstructor
public class FlyerLateralGabarit implements Gabarit {

    public static final String CODE = "flyer-lateral";

    private static final int[] BLANC = {255, 255, 255};
    private static final int[] CLAIR = {222, 226, 234};
    private static final float LARGEUR = PDRectangle.A5.getWidth();
    private static final float HAUTEUR = PDRectangle.A5.getHeight();

    private static final float STRIPE_W = LARGEUR * 0.42f;
    private static final float LG_X = 20f;                       // texte de la bande
    private static final float LG_W = STRIPE_W - 40f;
    private static final float RD_X = STRIPE_W + 22f;            // texte de la zone blanche
    private static final float RD_W = LARGEUR - 22f - RD_X;
    private static final float HAUT = HAUTEUR - 64f;

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
        return ContenuFlyer.PROMPT;
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

            int[] fond = foncer(s.primaire(), 74);
            int[] surTitre = eclaircir(s.accent(), 205);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                Cursor g = new Cursor(cs, HAUT);
                Cursor d = new Cursor(cs, HAUT);

                g.bandeau(0, 0, STRIPE_W, HAUTEUR, fond);   // bande colorée à gauche
                bandeGauche(g, flyer, s, surTitre);
                zoneDroite(d, flyer, s);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Échec du rendu du flyer", e);
        }
    }

    private void bandeGauche(Cursor g, ContenuFlyer flyer, Style s, int[] surTitre) throws IOException {
        String titre = nonVide(flyer.titre());
        float tTitre = tailleQuiTient(s.gras(), motLePlusLong(titre), 26, 15, LG_W);
        for (String ligne : envelopper(s.gras(), tTitre, titre, LG_W)) {
            g.texteCouleur(s.gras(), tTitre, LG_X, ligne, BLANC);
            g.avancer(tTitre + 4);
        }
        g.avancer(6);
        g.bandeau(LG_X, g.y, 30, 3, surTitre);
        g.avancer(18);
        for (String ligne : envelopper(s.normal(), 11.5f, nonVide(flyer.accroche()), LG_W)) {
            g.texteCouleur(s.normal(), 11.5f, LG_X, ligne, CLAIR);
            g.avancer(15);
        }
    }

    private void zoneDroite(Cursor d, ContenuFlyer flyer, Style s) throws IOException {
        if (flyer.blocs() != null) {
            for (ContenuFlyer.Bloc bloc : flyer.blocs()) {
                if (rempli(bloc.libelle())) {
                    for (String ligne : envelopper(s.gras(), 13, bloc.libelle(), RD_W)) {
                        d.texteCouleur(s.gras(), 13, RD_X, ligne, s.primaire());
                        d.avancer(17);
                    }
                }
                if (rempli(bloc.valeur())) {
                    for (String ligne : envelopper(s.normal(), 13, bloc.valeur(), RD_W)) {
                        d.texteCouleur(s.normal(), 13, RD_X, ligne, s.accent());
                        d.avancer(16);
                    }
                }
                d.avancer(10);
            }
        }

        // Contact au bas de la zone blanche
        List<String> lignes = contact(flyer.contact());
        float yPied = 40 + (lignes.size() - 1) * 15f;
        for (String ligne : lignes) {
            d.texteCouleurA(s.normal(), 9.5f, RD_X, yPied, ligne, s.texte());
            yPied -= 15;
        }
    }

    private List<String> contact(ContenuFlyer.Contact contact) {
        List<String> lignes = new ArrayList<>();
        if (contact == null) return lignes;
        if (rempli(contact.adresse())) lignes.add(contact.adresse());
        if (rempli(contact.telephone())) lignes.add(contact.telephone());
        if (rempli(contact.email())) lignes.add(contact.email());
        return lignes;
    }
}
