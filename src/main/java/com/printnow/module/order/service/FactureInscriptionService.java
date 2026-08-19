package com.printnow.module.order.service;

import com.printnow.module.order.service.pdf.PdfFactureHelpers.Cursor;
import com.printnow.module.shop.model.Imprimerie;
import com.printnow.module.shop.repository.ImprimerieRepository;
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
import java.time.format.DateTimeFormatter;

import static com.printnow.module.order.service.pdf.PdfFactureHelpers.*;

/**
 * Génère la facture PDF des frais d'inscription d'une imprimerie partenaire.
 *
 * À la différence de la facture client (où l'imprimerie est vendeuse) ou du
 * relevé de vente (qui n'est pas une facture), PrintNow est ici directement
 * vendeuse d'un service — l'accès à la plateforme — à l'imprimerie, qui agit
 * comme acheteuse professionnelle (elle fournit son propre numéro de TVA).
 *
 * Le numéro, la date et le montant sont ceux figés sur l'imprimerie au moment
 * de son inscription (voir Imprimerie.montantFraisInscription) : jamais le
 * tarif actuellement configuré, qui a pu changer depuis.
 */
@Service
@RequiredArgsConstructor
public class FactureInscriptionService {

    private static final DateTimeFormatter DATE_HEURE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ImprimerieRepository imprimerieRepository;

    /** gerantId == null signifie un appel admin (pas de vérification de propriété). */
    @Transactional(readOnly = true)
    public byte[] genererFacture(Long imprimerieId, Long gerantId) {
        Imprimerie imprimerie = imprimerieRepository.findById(imprimerieId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Imprimerie introuvable."));

        if (gerantId != null && (imprimerie.getGerant() == null || !imprimerie.getGerant().getId().equals(gerantId))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cette imprimerie ne vous appartient pas.");
        }
        if (imprimerie.getMontantFraisInscription() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Aucune facture d'inscription disponible pour cette imprimerie.");
        }

        return construirePdf(imprimerie);
    }

    /**
     * Produit la facture sans contrôle de propriété, pour l'envoi automatique
     * par email juste après l'inscription (voir PartnerRegistrationService).
     */
    public byte[] genererPourEnvoiEmail(Imprimerie imprimerie) {
        return construirePdf(imprimerie);
    }

    private byte[] construirePdf(Imprimerie imprimerie) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDImageXObject logoPrintNow = chargerLogoPrintNow(document);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                ecrire(cs, page, imprimerie, logoPrintNow);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Erreur lors de la génération de la facture d'inscription PDF", e);
        }
    }

    private void ecrire(PDPageContentStream cs, PDPage page, Imprimerie imprimerie, PDImageXObject logoPrintNow) throws IOException {
        float largeurPage = page.getMediaBox().getWidth();
        float largeurUtile = largeurPage - 2 * MARGE;
        Cursor c = new Cursor(cs, page.getMediaBox().getHeight() - MARGE);

        String numero = nonVide(imprimerie.getNumeroFactureInscription());
        String date = imprimerie.getDateInscription() != null
                ? imprimerie.getDateInscription().format(DATE_HEURE) : "";

        // En-tête : logo PrintNow à gauche (vendeuse), titre + n°/date à droite.
        float hauteurEntete = 32f;
        if (logoPrintNow != null) {
            c.dessinerImage(logoPrintNow, MARGE, c.y - hauteurEntete, hauteurEntete);
        }
        c.texteCouleurDroiteA(FONT_BOLD, 20, MARGE + largeurUtile, c.y - 16, "FACTURE", NAVY);
        c.texteCouleurDroiteA(FONT, 10, MARGE + largeurUtile, c.y - 32, "N° " + numero + "  ·  " + date, GRIS_TEXTE);

        c.y -= hauteurEntete;
        c.avancer(16);
        c.ligneHorizontale(MARGE, MARGE + largeurUtile);
        c.avancer(20);

        // Bloc vendeur / acheteur, sur deux colonnes de même hauteur
        float colonneDroite = MARGE + largeurUtile / 2 + 20;
        float yBloc = c.y;

        c.texteCouleurA(FONT_BOLD, 10, MARGE, yBloc, "VENDEUR", GRIS_TEXTE);
        c.texteA(FONT_BOLD, 10, MARGE, yBloc - 14, "PrintNow");
        c.texteA(FONT, 10, MARGE, yBloc - 27, "Plateforme de mise en relation");
        c.texteA(FONT, 10, MARGE, yBloc - 40, "contact@printnow.be");
        c.texteA(FONT, 10, MARGE, yBloc - 53, "Bruxelles, Belgique");

        c.texteCouleurA(FONT_BOLD, 10, colonneDroite, yBloc, "FACTURÉ À", GRIS_TEXTE);
        c.texteA(FONT_BOLD, 10, colonneDroite, yBloc - 14, nonVide(imprimerie.getNom()));
        c.texteA(FONT, 10, colonneDroite, yBloc - 27, nonVide(imprimerie.getAdresse()));
        c.texteA(FONT, 10, colonneDroite, yBloc - 40, nonVide(imprimerie.getVille()) + ", " + nonVide(imprimerie.getPays()));
        c.texteA(FONT, 10, colonneDroite, yBloc - 53,
                "TVA : " + (imprimerie.getNumeroTva() != null ? imprimerie.getNumeroTva() : "N/A"));

        c.y = yBloc - 53;
        c.avancer(24);
        c.ligneHorizontale(MARGE, MARGE + largeurUtile);
        c.avancer(24);

        // Ligne unique : les frais d'inscription (pas de quantité/prix unitaire,
        // inutiles pour un forfait fixe payé une seule fois)
        float xDesignation = MARGE;
        float xTotal = MARGE + largeurUtile - 70;

        c.bandeau(MARGE, c.y - 6, largeurUtile, 20, GRIS_CLAIR);
        c.texteCouleur(FONT_BOLD, 9, xDesignation, "DÉSIGNATION", GRIS_TEXTE);
        c.texteCouleur(FONT_BOLD, 9, xTotal, "TOTAL", GRIS_TEXTE);
        c.avancer(26);

        c.texte(FONT, 10, xDesignation, "Frais d'inscription — activation du compte partenaire PrintNow");
        c.texte(FONT, 10, xTotal, formatMontant(imprimerie.getMontantFraisInscription()));
        c.avancer(24);

        c.avancer(6);
        c.ligneHorizontale(MARGE, MARGE + largeurUtile);
        c.avancer(20);

        float xLabelTotal = MARGE + largeurUtile - 200;
        c.texteCouleur(FONT_BOLD, 12, xLabelTotal, "Total", ORANGE);
        c.texteCouleur(FONT_BOLD, 12, xTotal, formatMontant(imprimerie.getMontantFraisInscription()), ORANGE);

        c.avancer(40);

        c.ligneHorizontale(MARGE, MARGE + largeurUtile);
        c.avancer(16);
        String note = "Facture générée automatiquement par la plateforme PrintNow.";
        float largeurNote = FONT.getStringWidth(note) / 1000f * 8;
        c.texteCouleurA(FONT, 8, MARGE + (largeurUtile - largeurNote) / 2, c.y, note, GRIS_TEXTE);
    }
}
