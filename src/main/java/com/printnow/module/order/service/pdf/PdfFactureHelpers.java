package com.printnow.module.order.service.pdf;

import com.printnow.module.order.model.Commande;
import com.printnow.module.order.model.LigneCommande;
import com.printnow.module.shop.model.Produit;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Aides partagées par les différents générateurs de PDF de facturation
 * (facture client, relevé de commission imprimerie...) : dessin bas niveau,
 * formats d'affichage, chargement des logos.
 */
public final class PdfFactureHelpers {

    public static final float MARGE = 50f;
    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final PDType1Font FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    public static final PDType1Font FONT_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    // Couleurs de la charte PrintNow (mêmes teintes que le logo / le site).
    public static final int[] NAVY = {27, 41, 75};
    public static final int[] ORANGE = {245, 159, 10};
    public static final int[] GRIS_CLAIR = {245, 245, 247};
    public static final int[] GRIS_TEXTE = {110, 110, 120};

    private PdfFactureHelpers() {
    }

    public static String formatMontant(BigDecimal montant) {
        BigDecimal valeur = montant != null ? montant : BigDecimal.ZERO;
        return String.format(Locale.FRENCH, "%.2f €", valeur);
    }

    public static String formatEnum(String value) {
        String lower = value.replace("_", " ").toLowerCase(Locale.FRENCH);
        return lower.substring(0, 1).toUpperCase(Locale.FRENCH) + lower.substring(1);
    }

    public static String nonVide(String valeur) {
        return valeur != null ? valeur : "";
    }

    public static String formatDateFacture(Commande commande) {
        return commande.getDatePaiement() != null
                ? commande.getDatePaiement().format(DATE_FORMAT)
                : commande.getDateCreation().format(DATE_FORMAT);
    }

    public static String libelleLigne(LigneCommande ligne) {
        Produit produit = ligne.getProduit();
        StringBuilder sb = new StringBuilder(formatEnum(produit.getTypeProduit().name()));
        sb.append(" ").append(formatEnum(produit.getFormatImpression().name()));
        if (produit.getTypeProduit().name().equals("DOCUMENT")) {
            sb.append(Boolean.TRUE.equals(ligne.getCouleur()) ? " Couleur" : " N&B");
        }
        if (Boolean.TRUE.equals(ligne.getRectoVerso())) {
            sb.append(" (recto-verso)");
        }
        if (ligne.getReliure() != null && !ligne.getReliure().equals("AUCUNE")) {
            sb.append(" + reliure");
        }
        if (ligne.getFinition() != null && !ligne.getFinition().equals("AUCUNE")) {
            sb.append(" + plastification");
        }
        return sb.toString();
    }

    /** Charge une image stockée sur disque (ex: logo d'imprimerie) depuis son URL publique /uploads/... */
    public static PDImageXObject chargerLogoDepuisDisque(PDDocument document, String logoUrl, String uploadDir, String nomImage) {
        if (logoUrl == null || logoUrl.isBlank()) return null;
        try {
            String cheminRelatif = logoUrl.replaceFirst("^/?uploads/", "");
            Path chemin = Paths.get(uploadDir, cheminRelatif);
            if (!Files.exists(chemin)) return null;
            return PDImageXObject.createFromByteArray(document, Files.readAllBytes(chemin), nomImage);
        } catch (IOException e) {
            return null; // Un document reste utile sans logo plutôt que de faire échouer toute la génération.
        }
    }

    /** Charge le logo PrintNow embarqué dans l'application. Renvoie null s'il est introuvable. */
    public static PDImageXObject chargerLogoPrintNow(PDDocument document) {
        try (InputStream in = PdfFactureHelpers.class.getClassLoader().getResourceAsStream("branding/logo.png")) {
            if (in == null) return null;
            return PDImageXObject.createFromByteArray(document, in.readAllBytes(), "logo-printnow");
        } catch (IOException e) {
            return null;
        }
    }

    /** Curseur d'écriture : dessine texte/lignes/images sur une page PDF, en descendant depuis y. */
    public static class Cursor {
        private final PDPageContentStream cs;
        public float y;

        public Cursor(PDPageContentStream cs, float yDepart) {
            this.cs = cs;
            this.y = yDepart;
        }

        public void avancer(float hauteur) {
            y -= hauteur;
        }

        /** Écrit au curseur courant (colonne principale), sans avancer automatiquement. */
        public void texte(PDType1Font font, float taille, float x, String valeur) throws IOException {
            texteA(font, taille, x, y, valeur);
        }

        /** Écrit à une position Y explicite, sans toucher au curseur principal. */
        public void texteA(PDType1Font font, float taille, float x, float yAbsolu, String valeur) throws IOException {
            cs.beginText();
            cs.setFont(font, taille);
            cs.newLineAtOffset(x, yAbsolu);
            cs.showText(valeur);
            cs.endText();
        }

        /** Écrit un texte aligné à droite, se terminant à xDroite. */
        public void texteDroiteA(PDType1Font font, float taille, float xDroite, float yAbsolu, String valeur) throws IOException {
            float largeurTexte = font.getStringWidth(valeur) / 1000f * taille;
            texteA(font, taille, xDroite - largeurTexte, yAbsolu, valeur);
        }

        /** setNonStrokingColor(int,int,int) attend des composantes 0..1, pas 0..255. */
        private void definirCouleur(int[] rgb) throws IOException {
            cs.setNonStrokingColor(rgb[0] / 255f, rgb[1] / 255f, rgb[2] / 255f);
        }

        private void reinitialiserCouleur() throws IOException {
            cs.setNonStrokingColor(0f, 0f, 0f);
        }

        /** Écrit au curseur courant dans une couleur donnée, puis remet le noir par défaut. */
        public void texteCouleur(PDType1Font font, float taille, float x, String valeur, int[] rgb) throws IOException {
            definirCouleur(rgb);
            texte(font, taille, x, valeur);
            reinitialiserCouleur();
        }

        /** Écrit un texte aligné à droite dans une couleur donnée, puis remet le noir par défaut. */
        public void texteCouleurDroite(PDType1Font font, float taille, float xDroite, String valeur, int[] rgb) throws IOException {
            definirCouleur(rgb);
            texteDroiteA(font, taille, xDroite, y, valeur);
            reinitialiserCouleur();
        }

        /** Écrit à une position Y explicite dans une couleur donnée, puis remet le noir par défaut. */
        public void texteCouleurA(PDType1Font font, float taille, float x, float yAbsolu, String valeur, int[] rgb) throws IOException {
            definirCouleur(rgb);
            texteA(font, taille, x, yAbsolu, valeur);
            reinitialiserCouleur();
        }

        /** Écrit un texte aligné à droite à une position Y explicite dans une couleur donnée. */
        public void texteCouleurDroiteA(PDType1Font font, float taille, float xDroite, float yAbsolu, String valeur, int[] rgb) throws IOException {
            definirCouleur(rgb);
            texteDroiteA(font, taille, xDroite, yAbsolu, valeur);
            reinitialiserCouleur();
        }

        public void ligneHorizontale(float xGauche, float xDroite) throws IOException {
            cs.moveTo(xGauche, y);
            cs.lineTo(xDroite, y);
            cs.setLineWidth(0.5f);
            cs.stroke();
        }

        /** Dessine un bandeau rempli d'une couleur (ex: en-tête de tableau grisé), sans toucher au curseur texte. */
        public void bandeau(float xGauche, float yBas, float largeur, float hauteur, int[] rgb) throws IOException {
            definirCouleur(rgb);
            cs.addRect(xGauche, yBas, largeur, hauteur);
            cs.fill();
            reinitialiserCouleur();
        }

        public float ratio(PDImageXObject image) {
            return image.getWidth() / (float) image.getHeight();
        }

        public void dessinerImage(PDImageXObject image, float x, float yBas, float hauteur) throws IOException {
            cs.drawImage(image, x, yBas, hauteur * ratio(image), hauteur);
        }
    }
}
