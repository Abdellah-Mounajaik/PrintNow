package com.printnow.module.order.service;

import com.printnow.module.order.enums.StatutCommande;
import com.printnow.module.order.model.Commande;
import com.printnow.module.order.repository.CommandeRepository;
import com.printnow.module.order.service.pdf.PdfFactureHelpers.Cursor;
import com.printnow.module.shop.model.Imprimerie;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Set;

import static com.printnow.module.order.service.pdf.PdfFactureHelpers.*;

/**
 * Génère la facture de commission PDF d'une commande, côté PrintNow : le
 * document qui montre ce que la plateforme a gagné sur cette commande
 * (sa commission), indépendamment du montant reversé à l'imprimerie.
 * Réservée à l'admin.
 */
@Service
@RequiredArgsConstructor
public class FactureCommissionService {

    private static final Set<StatutCommande> STATUTS_FACTURABLES = EnumSet.of(
            StatutCommande.PAYEE, StatutCommande.EN_COURS_IMPRESSION,
            StatutCommande.PRETE, StatutCommande.LIVREE);

    private final CommandeRepository commandeRepository;

    @Transactional(readOnly = true)
    public byte[] genererFactureCommission(Long commandeId) {
        Commande commande = commandeRepository.findById(commandeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Commande non trouvée."));

        if (!STATUTS_FACTURABLES.contains(commande.getStatut())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Aucune facture disponible : cette commande n'est pas encore payée.");
        }

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDImageXObject logoPrintNow = chargerLogoPrintNow(document);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                ecrire(cs, page, commande, logoPrintNow);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Erreur lors de la génération de la facture de commission PDF", e);
        }
    }

    private void ecrire(PDPageContentStream cs, PDPage page, Commande commande, PDImageXObject logoPrintNow) throws IOException {
        float largeurPage = page.getMediaBox().getWidth();
        float largeurUtile = largeurPage - 2 * MARGE;
        Cursor c = new Cursor(cs, page.getMediaBox().getHeight() - MARGE);

        Imprimerie imprimerie = commande.getImprimerie();
        BigDecimal montantCommission = commande.getCommissionPlateforme() != null ? commande.getCommissionPlateforme() : BigDecimal.ZERO;
        String numeroFacture = commande.getNumeroCommande().replaceFirst("^CMD-", "COM-");

        // En-tête : logo PrintNow à gauche (émetteur), titre + n°/date à droite.
        float hauteurEntete = 32f;
        if (logoPrintNow != null) {
            c.dessinerImage(logoPrintNow, MARGE, c.y - hauteurEntete, hauteurEntete);
        }
        c.texteCouleurDroiteA(FONT_BOLD, 18, MARGE + largeurUtile, c.y - 16, "FACTURE COMMISSION", NAVY);
        c.texteCouleurDroiteA(FONT, 10, MARGE + largeurUtile, c.y - 32,
                "N° " + numeroFacture + "  ·  " + formatDateFacture(commande), GRIS_TEXTE);

        c.y -= hauteurEntete;
        c.avancer(16);
        c.ligneHorizontale(MARGE, MARGE + largeurUtile);
        c.avancer(20);

        // Bloc émetteur / destinataire, sur deux colonnes de même hauteur
        float colonneDroite = MARGE + largeurUtile / 2 + 20;
        float yBloc = c.y;

        c.texteCouleurA(FONT_BOLD, 10, MARGE, yBloc, "ÉMETTEUR", GRIS_TEXTE);
        c.texteA(FONT_BOLD, 10, MARGE, yBloc - 14, "PrintNow");
        c.texteA(FONT, 10, MARGE, yBloc - 27, "Plateforme de mise en relation");
        c.texteA(FONT, 10, MARGE, yBloc - 40, "contact@printnow.be");
        c.texteA(FONT, 10, MARGE, yBloc - 53, "Bruxelles, Belgique");

        c.texteCouleurA(FONT_BOLD, 10, colonneDroite, yBloc, "DESTINATAIRE (IMPRIMEUR)", GRIS_TEXTE);
        c.texteA(FONT_BOLD, 10, colonneDroite, yBloc - 14, nonVide(imprimerie.getNom()));
        c.texteA(FONT, 10, colonneDroite, yBloc - 27, nonVide(imprimerie.getAdresse()));
        c.texteA(FONT, 10, colonneDroite, yBloc - 40, nonVide(imprimerie.getVille()) + ", " + nonVide(imprimerie.getPays()));
        c.texteA(FONT, 10, colonneDroite, yBloc - 53,
                "TVA : " + (imprimerie.getNumeroTva() != null ? imprimerie.getNumeroTva() : "N/A"));

        c.y = yBloc - 53;
        c.avancer(24);
        c.ligneHorizontale(MARGE, MARGE + largeurUtile);
        c.avancer(24);

        // Tableau : une ligne unique, la commission elle-même (pas le détail des produits imprimés).
        float xDesignation = MARGE;
        float xQuantite = MARGE + largeurUtile - 260;
        float xPrixUnitaire = MARGE + largeurUtile - 170;
        float xTotal = MARGE + largeurUtile - 70;

        c.bandeau(MARGE, c.y - 6, largeurUtile, 20, GRIS_CLAIR);
        c.texteCouleur(FONT_BOLD, 9, xDesignation, "DÉSIGNATION", GRIS_TEXTE);
        c.texteCouleur(FONT_BOLD, 9, xQuantite, "QTÉ", GRIS_TEXTE);
        c.texteCouleur(FONT_BOLD, 9, xPrixUnitaire, "PRIX UNIT.", GRIS_TEXTE);
        c.texteCouleur(FONT_BOLD, 9, xTotal, "TOTAL", GRIS_TEXTE);
        c.avancer(26);

        c.texte(FONT, 10, xDesignation, "Commission sur vente");
        c.texte(FONT, 10, xQuantite, "1");
        c.texte(FONT, 10, xPrixUnitaire, formatMontant(montantCommission));
        c.texte(FONT, 10, xTotal, formatMontant(montantCommission));
        c.avancer(13);
        c.texteCouleur(FONT, 8, xDesignation,
                "Commande N° " + commande.getNumeroCommande() + "  ·  Taux : 10 %  ·  Base de calcul : " + formatMontant(commande.getTotalTTC()) + " (Vente TTC)",
                GRIS_TEXTE);
        c.avancer(24);

        c.ligneHorizontale(MARGE, MARGE + largeurUtile);
        c.avancer(20);

        // PrintNow est sous le régime de la franchise de la taxe (art. 56bis CTVA) :
        // pas de TVA sur ses propres prestations, d'où l'absence de ligne TVA ici
        // (à la différence de la facture client, où le vendeur est l'imprimerie).
        float xLabelTotal = MARGE + largeurUtile - 220;
        c.texteCouleur(FONT_BOLD, 12, xLabelTotal, "Total Commission", ORANGE);
        c.texteCouleur(FONT_BOLD, 12, xTotal, formatMontant(montantCommission), ORANGE);
        c.avancer(28);

        c.texteCouleur(FONT, 8, MARGE, "Petite entreprise soumise au régime de la franchise de la taxe.", GRIS_TEXTE);
        c.avancer(11);
        c.texteCouleur(FONT, 8, MARGE, "TVA non applicable (art. 56bis, CTVA).", GRIS_TEXTE);
        c.avancer(30);

        c.ligneHorizontale(MARGE, MARGE + largeurUtile);
        c.avancer(16);
        float largeurNote = FONT.getStringWidth("Facture générée automatiquement par la plateforme PrintNow.") / 1000f * 8;
        c.texteCouleurA(FONT, 8, MARGE + (largeurUtile - largeurNote) / 2, c.y,
                "Facture générée automatiquement par la plateforme PrintNow.", GRIS_TEXTE);
    }
}
