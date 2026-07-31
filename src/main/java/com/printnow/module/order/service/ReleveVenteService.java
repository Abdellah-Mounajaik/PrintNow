package com.printnow.module.order.service;

import com.printnow.module.order.enums.StatutCommande;
import com.printnow.module.order.model.Commande;
import com.printnow.module.order.model.LigneCommande;
import com.printnow.module.order.repository.CommandeRepository;
import com.printnow.module.order.service.pdf.PdfFactureHelpers.Cursor;
import com.printnow.module.shop.model.Imprimerie;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.EnumSet;
import java.util.Set;

import static com.printnow.module.order.service.pdf.PdfFactureHelpers.*;

/**
 * Génère le relevé de vente PDF d'une commande, côté imprimerie : le document
 * qui montre ce que l'imprimerie a réellement perçu pour cette commande, une
 * fois la commission PrintNow déduite. Consultable par l'admin (n'importe
 * quelle commande) ou par l'imprimeur propriétaire de la commande.
 */
@Service
@RequiredArgsConstructor
public class ReleveVenteService {

    private static final Set<StatutCommande> STATUTS_FACTURABLES = EnumSet.of(
            StatutCommande.PAYEE, StatutCommande.EN_COURS_IMPRESSION,
            StatutCommande.PRETE, StatutCommande.LIVREE);

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private final CommandeRepository commandeRepository;

    /** gerantId == null signifie un appel admin (pas de vérification de propriété). */
    @Transactional(readOnly = true)
    public byte[] genererReleveVente(Long commandeId, Long gerantId) {
        Commande commande = commandeRepository.findById(commandeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Commande non trouvée."));

        if (gerantId != null && !commande.getImprimerie().getGerant().getId().equals(gerantId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cette commande ne concerne pas votre imprimerie.");
        }
        if (!STATUTS_FACTURABLES.contains(commande.getStatut())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Aucun relevé disponible : cette commande n'est pas encore payée.");
        }

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDImageXObject logoImprimerie = chargerLogoDepuisDisque(
                    document, commande.getImprimerie().getLogoUrl(), uploadDir, "logo-imprimerie");
            PDImageXObject logoPrintNow = chargerLogoPrintNow(document);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                ecrire(cs, page, commande, logoImprimerie, logoPrintNow);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Erreur lors de la génération du relevé de vente PDF", e);
        }
    }

    private void ecrire(PDPageContentStream cs, PDPage page, Commande commande,
                         PDImageXObject logoImprimerie, PDImageXObject logoPrintNow) throws IOException {
        float largeurPage = page.getMediaBox().getWidth();
        float largeurUtile = largeurPage - 2 * MARGE;
        Cursor c = new Cursor(cs, page.getMediaBox().getHeight() - MARGE);

        Imprimerie imprimerie = commande.getImprimerie();

        // En-tête : logo PrintNow bien visible à gauche (émetteur du document),
        // titre + n°/date à droite.
        float hauteurEntete = 42f;
        if (logoPrintNow != null) {
            c.dessinerImage(logoPrintNow, MARGE, c.y - hauteurEntete, hauteurEntete);
        }
        c.texteDroiteA(FONT_BOLD, 18, MARGE + largeurUtile, c.y - 18, "RELEVÉ DE VENTE");
        c.texteDroiteA(FONT, 10, MARGE + largeurUtile, c.y - 34,
                "Commande N° " + commande.getNumeroCommande() + "  ·  " + formatDateFacture(commande));

        c.y -= hauteurEntete;
        c.avancer(16);
        c.ligneHorizontale(MARGE, MARGE + largeurUtile);
        c.avancer(16);

        // Bloc émetteur / destinataire, sur deux colonnes de même hauteur
        float colonneDroite = MARGE + largeurUtile / 2 + 20;
        float yBloc = c.y;

        c.texteA(FONT_BOLD, 11, MARGE, yBloc, "Émetteur");
        c.texteA(FONT, 10, MARGE, yBloc - 14, "PrintNow");

        c.texteA(FONT_BOLD, 11, colonneDroite, yBloc, "Destinataire");
        c.texteA(FONT, 10, colonneDroite, yBloc - 14, nonVide(imprimerie.getNom()));
        c.texteA(FONT, 10, colonneDroite, yBloc - 27, nonVide(imprimerie.getAdresse()));
        c.texteA(FONT, 10, colonneDroite, yBloc - 40, nonVide(imprimerie.getVille()) + ", " + nonVide(imprimerie.getPays()));
        c.texteA(FONT, 10, colonneDroite, yBloc - 53,
                "TVA : " + (imprimerie.getNumeroTva() != null ? imprimerie.getNumeroTva() : "N/A"));

        c.y = yBloc - 53;
        c.avancer(24);
        c.ligneHorizontale(MARGE, MARGE + largeurUtile);
        c.avancer(20);

        // Détail de la vente, pour justifier le montant perçu
        float xDesignation = MARGE;
        float xQuantite = MARGE + largeurUtile - 260;
        float xPrixUnitaire = MARGE + largeurUtile - 170;
        float xTotal = MARGE + largeurUtile - 70;

        c.texte(FONT_BOLD, 10, xDesignation, "Désignation");
        c.texte(FONT_BOLD, 10, xQuantite, "Qté");
        c.texte(FONT_BOLD, 10, xPrixUnitaire, "Prix unit.");
        c.texte(FONT_BOLD, 10, xTotal, "Total");
        c.avancer(6);
        c.ligneHorizontale(MARGE, MARGE + largeurUtile);
        c.avancer(16);

        for (LigneCommande ligne : commande.getLignes()) {
            c.texte(FONT, 10, xDesignation, libelleLigne(ligne));
            c.texte(FONT, 10, xQuantite, String.valueOf(ligne.getQuantite()));
            c.texte(FONT, 10, xPrixUnitaire, formatMontant(ligne.getPrixUnitaire()));
            c.texte(FONT, 10, xTotal, formatMontant(ligne.getPrixTotal()));
            c.avancer(18);
        }
        if (commande.getFraisExpress() != null) {
            c.texte(FONT, 10, xDesignation, "Impression express 2h");
            c.texte(FONT, 10, xTotal, formatMontant(commande.getFraisExpress()));
            c.avancer(18);
        }
        if (commande.getFraisLivraison() != null) {
            c.texte(FONT, 10, xDesignation, "Livraison à domicile");
            c.texte(FONT, 10, xTotal, formatMontant(commande.getFraisLivraison()));
            c.avancer(18);
        }

        c.avancer(6);
        c.ligneHorizontale(MARGE, MARGE + largeurUtile);
        c.avancer(20);

        // Décompte : vente → commission retenue → net perçu (le gain de l'imprimerie)
        float xLabelTotal = MARGE + largeurUtile - 220;
        c.texte(FONT, 10, xLabelTotal, "Montant de la vente (TTC)");
        c.texte(FONT, 10, xTotal, formatMontant(commande.getTotalTTC()));
        c.avancer(15);

        c.texte(FONT, 10, xLabelTotal, "Commission retenue (10%)");
        c.texte(FONT, 10, xTotal, "-" + formatMontant(commande.getCommissionPlateforme()));
        c.avancer(19);

        c.texte(FONT_BOLD, 11, xLabelTotal, "Montant net perçu");
        c.texte(FONT_BOLD, 11, xTotal, formatMontant(commande.getMontantVerseImprimerie()));
        c.avancer(40);

        if (logoImprimerie != null) {
            float hauteurLogoFooter = 16f;
            c.dessinerImage(logoImprimerie, MARGE, c.y - hauteurLogoFooter + 4, hauteurLogoFooter);
            c.texteA(FONT, 8, MARGE + hauteurLogoFooter * c.ratio(logoImprimerie) + 8, c.y - 8,
                    "Relevé généré automatiquement par PrintNow.");
        } else {
            c.texte(FONT, 8, MARGE, "Relevé généré automatiquement par PrintNow.");
        }
    }
}
