package com.printnow.module.order.service;

import com.printnow.module.order.enums.ModeRetrait;
import com.printnow.module.order.enums.StatutCommande;
import com.printnow.module.order.model.AdresseLivraison;
import com.printnow.module.order.model.Commande;
import com.printnow.module.order.model.LigneCommande;
import com.printnow.module.order.repository.CommandeRepository;
import com.printnow.module.order.service.pdf.PdfFactureHelpers.Cursor;
import com.printnow.module.promo.model.CodePromo;
import com.printnow.module.shop.model.Imprimerie;
import com.printnow.module.user.model.User;
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
 * Génère la facture PDF d'une commande, à la demande (pas de table "facture"
 * séparée en base : toutes les données nécessaires existent déjà sur la
 * Commande, ses lignes, l'imprimerie et le client). L'imprimerie reste le
 * vendeur légal ; PrintNow n'apparaît qu'en tant que plateforme émettrice
 * du document.
 */
@Service
@RequiredArgsConstructor
public class FactureService {

    private static final Set<StatutCommande> STATUTS_FACTURABLES = EnumSet.of(
            StatutCommande.PAYEE, StatutCommande.EN_COURS_IMPRESSION,
            StatutCommande.PRETE, StatutCommande.LIVREE);

    private final CommandeRepository commandeRepository;
    private final DepotFacturesArchivees depotArchives;

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

        return construirePdf(commande);
    }

    /**
     * Produit la même facture, à destination de l'imprimeur qui a honoré la
     * commande.
     *
     * C'est l'imprimerie qui vend au client et au nom de qui la facture est
     * émise : elle doit pouvoir en conserver une copie, comme sa comptabilité
     * l'exige. Le nom du client y figure donc, à la différence du relevé de
     * vente et de la facture de commission, qui ne concernent qu'elle et
     * PrintNow.
     */
    @Transactional(readOnly = true)
    public byte[] genererFacturePourImprimeur(Long commandeId, Long gerantId) {
        Commande commande = commandeRepository.findById(commandeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Commande non trouvée."));

        if (commande.getImprimerie() == null || commande.getImprimerie().getGerant() == null
                || !commande.getImprimerie().getGerant().getId().equals(gerantId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cette commande ne concerne pas votre imprimerie.");
        }
        if (!STATUTS_FACTURABLES.contains(commande.getStatut())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Aucune facture disponible : cette commande n'est pas encore payée.");
        }

        // Si le client a supprimé son compte, la facture a été figée avant que
        // son nom ne disparaisse : c'est cette copie-là qui a valeur comptable,
        // la régénérer donnerait une facture sans destinataire identifiable.
        return depotArchives.lire(commande.getNumeroCommande())
                .orElseGet(() -> construirePdf(commande));
    }

    /** Une commande donne-t-elle lieu à une facture ? */
    public boolean estFacturable(Commande commande) {
        return STATUTS_FACTURABLES.contains(commande.getStatut());
    }

    /**
     * Produit la facture sans contrôler à qui elle appartient.
     *
     * Réservé à l'archivage au moment de la suppression d'un compte : la facture
     * doit y être figée telle qu'elle est, avant que le nom du client ne
     * disparaisse de la base (voir ArchiveFactureService).
     */
    @Transactional(readOnly = true)
    public byte[] genererPourArchivage(Commande commande) {
        return construirePdf(commande);
    }

    private byte[] construirePdf(Commande commande) {
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
            throw new UncheckedIOException("Erreur lors de la génération de la facture PDF", e);
        }
    }

    private void ecrire(PDPageContentStream cs, PDPage page, Commande commande, PDImageXObject logoPrintNow) throws IOException {
        float largeurPage = page.getMediaBox().getWidth();
        float largeurUtile = largeurPage - 2 * MARGE;
        Cursor c = new Cursor(cs, page.getMediaBox().getHeight() - MARGE);

        Imprimerie imprimerie = commande.getImprimerie();
        User client = commande.getClient();

        // En-tête : logo PrintNow à gauche (la plateforme émet le document),
        // titre + n°/date à droite, hauteur réservée fixe que le logo soit présent ou non.
        float hauteurEntete = 32f;
        if (logoPrintNow != null) {
            c.dessinerImage(logoPrintNow, MARGE, c.y - hauteurEntete, hauteurEntete);
        }
        c.texteCouleurDroiteA(FONT_BOLD, 20, MARGE + largeurUtile, c.y - 16, "FACTURE", NAVY);
        c.texteCouleurDroiteA(FONT, 10, MARGE + largeurUtile, c.y - 32,
                "N° " + commande.getNumeroCommande() + "  ·  " + formatDateFacture(commande), GRIS_TEXTE);

        c.y -= hauteurEntete;
        c.avancer(16);
        c.ligneHorizontale(MARGE, MARGE + largeurUtile);
        c.avancer(20);

        // Bloc vendeur / client, sur deux colonnes de même hauteur (positions absolues)
        float colonneDroite = MARGE + largeurUtile / 2 + 20;
        float yBloc = c.y;

        c.texteCouleurA(FONT_BOLD, 10, MARGE, yBloc, "VENDEUR", GRIS_TEXTE);
        c.texteA(FONT_BOLD, 10, MARGE, yBloc - 14, nonVide(imprimerie.getNom()));
        c.texteA(FONT, 10, MARGE, yBloc - 27, nonVide(imprimerie.getAdresse()));
        c.texteA(FONT, 10, MARGE, yBloc - 40, nonVide(imprimerie.getVille()) + ", " + nonVide(imprimerie.getPays()));
        c.texteA(FONT, 10, MARGE, yBloc - 53,
                "TVA : " + (imprimerie.getNumeroTva() != null ? imprimerie.getNumeroTva() : "N/A"));

        c.texteCouleurA(FONT_BOLD, 10, colonneDroite, yBloc, "FACTURÉ À", GRIS_TEXTE);
        c.texteA(FONT_BOLD, 10, colonneDroite, yBloc - 14, nonVide(client.getPrenom()) + " " + nonVide(client.getNom()));
        c.texteA(FONT, 10, colonneDroite, yBloc - 27, nonVide(client.getEmail()));

        AdresseLivraison adresse = commande.getAdresseLivraison();
        if (commande.getModeRetrait() == ModeRetrait.LIVRAISON && adresse != null) {
            c.texteA(FONT, 10, colonneDroite, yBloc - 40, adresse.getNumero() + " " + adresse.getRue());
            c.texteA(FONT, 10, colonneDroite, yBloc - 53,
                    adresse.getCodePostal() + " " + adresse.getVille() + ", " + adresse.getPays());
        } else {
            c.texteA(FONT, 10, colonneDroite, yBloc - 40, "Retrait en magasin");
        }

        c.y = yBloc - 53;
        c.avancer(24);
        c.ligneHorizontale(MARGE, MARGE + largeurUtile);
        c.avancer(24);

        // En-tête du tableau des lignes, sur un bandeau grisé
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

        for (LigneCommande ligne : commande.getLignes()) {
            c.texte(FONT, 10, xDesignation, libelleLigne(ligne));
            c.texte(FONT, 10, xQuantite, String.valueOf(ligne.getQuantite()));
            c.texte(FONT, 10, xPrixUnitaire, formatMontant(ligne.getPrixUnitaire()));
            c.texte(FONT, 10, xTotal, formatMontant(ligne.getPrixTotal()));
            c.avancer(18);
        }

        // Options de la commande (express, livraison) affichées comme des lignes
        // à part, puisqu'elles ne sont pas rattachées à un produit précis.
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

        // Totaux, alignés à droite
        float xLabelTotal = MARGE + largeurUtile - 200;
        ligneTotal(c, xLabelTotal, xTotal, "Sous-total HT", commande.getTotalHT());

        if (commande.getMontantReductionEtudiant() != null && commande.getMontantReductionEtudiant().signum() > 0) {
            ligneTotal(c, xLabelTotal, xTotal, "Réduction étudiant", commande.getMontantReductionEtudiant().negate());
        }
        CodePromo promo = commande.getCodePromo();
        if (promo != null && commande.getMontantReduction() != null && commande.getMontantReduction().signum() > 0) {
            ligneTotal(c, xLabelTotal, xTotal, "Code promo (" + promo.getCode() + ")",
                    commande.getMontantReduction().negate());
        }
        ligneTotal(c, xLabelTotal, xTotal, "TVA (21%)", commande.getTotalTVA());

        // La vérification orthographique est vendue par PrintNow, non par
        // l'imprimerie : elle est présentée à part, sous son propre régime de TVA.
        // Sans elle, le total de la facture ne correspondrait pas à la somme
        // réellement débitée — le client verrait moins que ce qu'il a payé.
        BigDecimal corrections = commande.getMontantCorrections();
        BigDecimal generations = commande.getMontantGenerations();
        boolean avecCorrections = corrections != null && corrections.signum() > 0;
        boolean avecGenerations = generations != null && generations.signum() > 0;
        boolean avecSupplements = avecCorrections || avecGenerations;
        if (avecSupplements) {
            ligneTotal(c, xLabelTotal, xTotal, "Impression TTC", commande.getTotalTTC());
            if (avecCorrections) ligneTotal(c, xLabelTotal, xTotal, "Vérification orthographique", corrections);
            if (avecGenerations) ligneTotal(c, xLabelTotal, xTotal, "Design IA", generations);
        }

        BigDecimal totalPaye = commande.getTotalTTC();
        if (avecCorrections) totalPaye = totalPaye.add(corrections);
        if (avecGenerations) totalPaye = totalPaye.add(generations);

        c.avancer(6);
        c.texteCouleur(FONT_BOLD, 12, xLabelTotal, avecSupplements ? "Total payé" : "Total TTC", ORANGE);
        c.texteCouleur(FONT_BOLD, 12, xTotal, formatMontant(totalPaye), ORANGE);

        c.avancer(40);

        c.ligneHorizontale(MARGE, MARGE + largeurUtile);
        c.avancer(16);
        float largeurNote = FONT.getStringWidth("Facture générée automatiquement par la plateforme PrintNow.") / 1000f * 8;
        c.texteCouleurA(FONT, 8, MARGE + (largeurUtile - largeurNote) / 2, c.y,
                "Facture générée automatiquement par la plateforme PrintNow.", GRIS_TEXTE);
    }

    private void ligneTotal(Cursor c, float xLabel, float xValeur, String label, BigDecimal montant) throws IOException {
        c.texte(FONT, 10, xLabel, label);
        c.texte(FONT, 10, xValeur, formatMontant(montant));
        c.avancer(15);
    }
}
