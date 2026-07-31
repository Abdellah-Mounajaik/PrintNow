package com.printnow.module.order.service;

import com.printnow.module.order.enums.ModeRetrait;
import com.printnow.module.order.enums.StatutCommande;
import com.printnow.module.order.model.AdresseLivraison;
import com.printnow.module.order.model.Commande;
import com.printnow.module.order.model.LigneCommande;
import com.printnow.module.order.repository.CommandeRepository;
import com.printnow.module.promo.model.CodePromo;
import com.printnow.module.shop.model.Imprimerie;
import com.printnow.module.shop.model.Produit;
import com.printnow.module.user.model.User;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.util.Matrix;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * Génère la facture PDF d'une commande, à la demande (pas de table "facture"
 * séparée en base : toutes les données nécessaires existent déjà sur la
 * Commande, ses lignes, l'imprimerie et le client).
 */
@Service
@RequiredArgsConstructor
public class FactureService {

    private static final Set<StatutCommande> STATUTS_FACTURABLES = EnumSet.of(
            StatutCommande.PAYEE, StatutCommande.EN_COURS_IMPRESSION,
            StatutCommande.PRETE, StatutCommande.LIVREE);

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final float MARGE = 50f;
    private static final PDType1Font FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDType1Font FONT_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private final CommandeRepository commandeRepository;

    @Transactional(readOnly = true)
    public byte[] genererFacture(Long commandeId, Long clientId) {
        Commande commande = commandeRepository.findById(commandeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Commande non trouvée."));

        if (!commande.getClient().getId().equals(clientId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cette commande ne vous appartient pas.");
        }
        if (!STATUTS_FACTURABLES.contains(commande.getStatut())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Aucune facture disponible : cette commande n'est pas encore payée.");
        }

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDImageXObject logoImprimerie = chargerLogoImprimerie(document, commande.getImprimerie());
            PDImageXObject logoPrintNow = chargerLogoPrintNow(document);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                new FacturePageWriter(cs, page).ecrire(commande, logoImprimerie, logoPrintNow);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Erreur lors de la génération de la facture PDF", e);
        }
    }

    /** Charge le logo de l'imprimerie depuis le disque. Renvoie null s'il est absent ou illisible. */
    private PDImageXObject chargerLogoImprimerie(PDDocument document, Imprimerie imprimerie) {
        String logoUrl = imprimerie.getLogoUrl();
        if (logoUrl == null || logoUrl.isBlank()) return null;
        try {
            String cheminRelatif = logoUrl.replaceFirst("^/?uploads/", "");
            Path chemin = Paths.get(uploadDir, cheminRelatif);
            if (!Files.exists(chemin)) return null;
            return PDImageXObject.createFromByteArray(document, Files.readAllBytes(chemin), "logo-imprimerie");
        } catch (IOException e) {
            return null; // Une facture reste utile sans logo plutôt que de faire échouer toute la génération.
        }
    }

    /** Charge le logo PrintNow embarqué dans l'application. Renvoie null s'il est introuvable. */
    private PDImageXObject chargerLogoPrintNow(PDDocument document) {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("branding/logo.png")) {
            if (in == null) return null;
            return PDImageXObject.createFromByteArray(document, in.readAllBytes(), "logo-printnow");
        } catch (IOException e) {
            return null;
        }
    }

    /** Dessine le contenu de la facture sur une page, du haut vers le bas. */
    private static class FacturePageWriter {
        private final PDPageContentStream cs;
        private final float largeurUtile;
        private final float largeurPage;
        private float y;

        FacturePageWriter(PDPageContentStream cs, PDPage page) {
            this.cs = cs;
            this.largeurPage = page.getMediaBox().getWidth();
            this.largeurUtile = largeurPage - 2 * MARGE;
            this.y = page.getMediaBox().getHeight() - MARGE;
        }

        void ecrire(Commande commande, PDImageXObject logoImprimerie, PDImageXObject logoPrintNow) throws IOException {
            Imprimerie imprimerie = commande.getImprimerie();
            User client = commande.getClient();

            // En-tête : logo de l'imprimerie à gauche, titre + n°/date à droite,
            // hauteur réservée fixe que le logo soit présent ou non.
            float hauteurEntete = 42f;
            if (logoImprimerie != null) {
                dessinerImage(logoImprimerie, MARGE, y - hauteurEntete, hauteurEntete);
            }
            texteDroiteA(FONT_BOLD, 20, MARGE + largeurUtile, y - 18, "FACTURE");
            texteDroiteA(FONT, 10, MARGE + largeurUtile, y - 34,
                    "N° " + commande.getNumeroCommande() + "  ·  " + formatDate(commande));

            y -= hauteurEntete;
            avancer(16);
            ligneHorizontale();
            avancer(16);

            // Bloc vendeur / client, sur deux colonnes de même hauteur (positions absolues)
            float colonneDroite = MARGE + largeurUtile / 2 + 20;
            float yBloc = y;

            texteA(FONT_BOLD, 11, MARGE, yBloc, "Vendeur");
            texteA(FONT, 10, MARGE, yBloc - 14, nonVide(imprimerie.getNom()));
            texteA(FONT, 10, MARGE, yBloc - 27, nonVide(imprimerie.getAdresse()));
            texteA(FONT, 10, MARGE, yBloc - 40, nonVide(imprimerie.getVille()) + ", " + nonVide(imprimerie.getPays()));
            texteA(FONT, 10, MARGE, yBloc - 53,
                    "TVA : " + (imprimerie.getNumeroTva() != null ? imprimerie.getNumeroTva() : "N/A"));

            texteA(FONT_BOLD, 11, colonneDroite, yBloc, "Client");
            texteA(FONT, 10, colonneDroite, yBloc - 14, nonVide(client.getPrenom()) + " " + nonVide(client.getNom()));
            texteA(FONT, 10, colonneDroite, yBloc - 27, nonVide(client.getEmail()));

            AdresseLivraison adresse = commande.getAdresseLivraison();
            if (commande.getModeRetrait() == ModeRetrait.LIVRAISON && adresse != null) {
                texteA(FONT, 10, colonneDroite, yBloc - 40, adresse.getNumero() + " " + adresse.getRue());
                texteA(FONT, 10, colonneDroite, yBloc - 53,
                        adresse.getCodePostal() + " " + adresse.getVille() + ", " + adresse.getPays());
            } else {
                texteA(FONT, 10, colonneDroite, yBloc - 40, "Retrait en magasin");
            }

            y = yBloc - 53;
            avancer(24);
            ligneHorizontale();
            avancer(20);

            // En-tête du tableau des lignes
            float xDesignation = MARGE;
            float xQuantite = MARGE + largeurUtile - 260;
            float xPrixUnitaire = MARGE + largeurUtile - 170;
            float xTotal = MARGE + largeurUtile - 70;

            texte(FONT_BOLD, 10, xDesignation, "Désignation");
            texte(FONT_BOLD, 10, xQuantite, "Qté");
            texte(FONT_BOLD, 10, xPrixUnitaire, "Prix unit.");
            texte(FONT_BOLD, 10, xTotal, "Total");
            avancer(6);
            ligneHorizontale();
            avancer(16);

            for (LigneCommande ligne : commande.getLignes()) {
                texte(FONT, 10, xDesignation, libelleLigne(ligne));
                texte(FONT, 10, xQuantite, String.valueOf(ligne.getQuantite()));
                texte(FONT, 10, xPrixUnitaire, formatMontant(ligne.getPrixUnitaire()));
                texte(FONT, 10, xTotal, formatMontant(ligne.getPrixTotal()));
                avancer(18);
            }

            // Options de la commande (express, livraison) affichées comme des lignes
            // à part, puisqu'elles ne sont pas rattachées à un produit précis.
            if (commande.getFraisExpress() != null) {
                texte(FONT, 10, xDesignation, "Impression express 2h");
                texte(FONT, 10, xTotal, formatMontant(commande.getFraisExpress()));
                avancer(18);
            }
            if (commande.getFraisLivraison() != null) {
                texte(FONT, 10, xDesignation, "Livraison à domicile");
                texte(FONT, 10, xTotal, formatMontant(commande.getFraisLivraison()));
                avancer(18);
            }

            avancer(6);
            ligneHorizontale();
            avancer(20);

            // Totaux, alignés à droite
            float xLabelTotal = MARGE + largeurUtile - 200;
            ligneTotal(xLabelTotal, xTotal, "Sous-total HT", commande.getTotalHT());

            if (commande.getMontantReductionEtudiant() != null && commande.getMontantReductionEtudiant().signum() > 0) {
                ligneTotal(xLabelTotal, xTotal, "Réduction étudiant", commande.getMontantReductionEtudiant().negate());
            }
            CodePromo promo = commande.getCodePromo();
            if (promo != null && commande.getMontantReduction() != null && commande.getMontantReduction().signum() > 0) {
                ligneTotal(xLabelTotal, xTotal, "Code promo (" + promo.getCode() + ")",
                        commande.getMontantReduction().negate());
            }
            ligneTotal(xLabelTotal, xTotal, "TVA (20%)", commande.getTotalTVA());
            avancer(4);
            texte(FONT_BOLD, 11, xLabelTotal, "Total TTC");
            texte(FONT_BOLD, 11, xTotal, formatMontant(commande.getTotalTTC()));
            avancer(40);

            if (logoPrintNow != null) {
                float hauteurLogoFooter = 16f;
                dessinerImage(logoPrintNow, MARGE, y - hauteurLogoFooter + 4, hauteurLogoFooter);
                texteA(FONT, 8, MARGE + hauteurLogoFooter * ratio(logoPrintNow) + 8, y - 8,
                        "Facture générée automatiquement.");
            } else {
                texte(FONT, 8, MARGE, "Facture générée automatiquement par PrintNow.");
            }
        }

        private void ligneTotal(float xLabel, float xValeur, String label, BigDecimal montant) throws IOException {
            texte(FONT, 10, xLabel, label);
            texte(FONT, 10, xValeur, formatMontant(montant));
            avancer(15);
        }

        private String libelleLigne(LigneCommande ligne) {
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

        private String formatDate(Commande commande) {
            return commande.getDatePaiement() != null
                    ? commande.getDatePaiement().format(DATE_FORMAT)
                    : commande.getDateCreation().format(DATE_FORMAT);
        }

        private String formatEnum(String value) {
            String lower = value.replace("_", " ").toLowerCase(Locale.FRENCH);
            return lower.substring(0, 1).toUpperCase(Locale.FRENCH) + lower.substring(1);
        }

        private String formatMontant(BigDecimal montant) {
            BigDecimal valeur = montant != null ? montant : BigDecimal.ZERO;
            return String.format(Locale.FRENCH, "%.2f €", valeur);
        }

        private String nonVide(String valeur) {
            return valeur != null ? valeur : "";
        }

        private float ratio(PDImageXObject image) {
            return image.getWidth() / (float) image.getHeight();
        }

        private void dessinerImage(PDImageXObject image, float x, float yBas, float hauteur) throws IOException {
            cs.drawImage(image, x, yBas, hauteur * ratio(image), hauteur);
        }

        /** Écrit au curseur courant (colonne principale), puis avance via avancer(). */
        private void texte(PDType1Font font, float taille, float x, String valeur) throws IOException {
            texteA(font, taille, x, y, valeur);
        }

        /** Écrit à une position Y explicite, sans toucher au curseur principal. */
        private void texteA(PDType1Font font, float taille, float x, float yAbsolu, String valeur) throws IOException {
            cs.beginText();
            cs.setFont(font, taille);
            cs.newLineAtOffset(x, yAbsolu);
            cs.showText(valeur);
            cs.endText();
        }

        /** Écrit un texte aligné à droite, se terminant à xDroite. */
        private void texteDroiteA(PDType1Font font, float taille, float xDroite, float yAbsolu, String valeur) throws IOException {
            float largeurTexte = font.getStringWidth(valeur) / 1000f * taille;
            texteA(font, taille, xDroite - largeurTexte, yAbsolu, valeur);
        }

        private void avancer(float hauteur) {
            y -= hauteur;
        }

        private void ligneHorizontale() throws IOException {
            cs.moveTo(MARGE, y);
            cs.lineTo(MARGE + largeurUtile, y);
            cs.setLineWidth(0.5f);
            cs.stroke();
        }
    }
}
