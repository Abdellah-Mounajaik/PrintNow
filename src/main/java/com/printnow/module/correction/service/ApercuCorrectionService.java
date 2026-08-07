package com.printnow.module.correction.service;

import com.printnow.module.correction.dto.FauteDTO;
import com.printnow.module.correction.model.VerificationOrthographe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationHighlight;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Aperçu du document corrigé, proposé avant paiement.
 *
 * L'aperçu est délivré sous forme d'image filigranée, jamais en PDF : le client
 * constate le résultat dans sa mise en page sans obtenir de fichier
 * exploitable. La résolution est volontairement modeste et le filigrane couvre
 * la page, de sorte qu'une capture d'écran ne remplace pas le document acheté.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApercuCorrectionService {

    /** Résolution suffisante pour juger du rendu, insuffisante pour imprimer. */
    private static final int DPI_APERCU = 72;

    private final CorrectionService correctionService;
    private final CorrecteurPdfService correcteurPdf;

    /**
     * @param page      numéro de page demandé (1 = première)
     * @param ignorees  fautes que le client a choisi de laisser telles quelles
     * @param choix     suggestions qu'il a préférées à celle proposée par défaut
     * @return l'image PNG de la page corrigée, filigranée
     */
    public byte[] genererApercu(VerificationOrthographe verification, int page,
                                List<Integer> ignorees, Map<Integer, String> choix) {
        // Mêmes règles de sélection que pour le PDF final : l'aperçu montre
        // exactement ce que le client recevra.
        List<FauteDTO> fautes = correctionService.retenir(
                correctionService.fautesDe(verification), ignorees, choix);

        try (PDDocument document = Loader.loadPDF(Paths.get(verification.getCheminOriginal()).toFile())) {
            if (page < 1 || page > document.getNumberOfPages()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page inexistante.");
            }

            // Les corrections sont appliquées à une copie en mémoire : rien n'est
            // écrit sur disque tant que la commande n'est pas réglée.
            correcteurPdf.corriger(document, fautes);
            surlignerCorrections(document, page, fautes);

            PDFRenderer moteur = new PDFRenderer(document);
            BufferedImage image = moteur.renderImageWithDPI(page - 1, DPI_APERCU, ImageType.RGB);
            apposerFiligrane(image);

            ByteArrayOutputStream sortie = new ByteArrayOutputStream();
            ImageIO.write(image, "png", sortie);
            return sortie.toByteArray();

        } catch (ResponseStatusException e) {
            throw e;
        } catch (IOException e) {
            log.warn("Aperçu impossible pour la vérification {}", verification.getId(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Aperçu indisponible.");
        }
    }

    /** Première page comportant une faute : afficher la page 1 n'aurait sinon aucun intérêt. */
    public int premierePageAvecFaute(VerificationOrthographe verification) {
        return correctionService.fautesDe(verification).stream()
                .map(FauteDTO::getPage)
                .filter(p -> p != null && p > 0)
                .min(Integer::compareTo)
                .orElse(1);
    }

    /**
     * Met en évidence, en vert, les mots effectivement réécrits sur la page.
     *
     * Le surlignage n'existe que dans l'aperçu : il est appliqué à la copie en
     * mémoire, jamais au document livré après paiement.
     */
    private void surlignerCorrections(PDDocument document, int page, List<FauteDTO> fautes) {
        List<String> motsCorriges = new ArrayList<>();
        for (FauteDTO faute : fautes) {
            if (!Boolean.TRUE.equals(faute.getCorrigeeDansPdf())) continue;
            if (faute.getPage() == null || faute.getPage() != page) continue;
            motsCorriges.addAll(motsModifies(faute.getMotFautif(), faute.getCorrection()));
        }
        if (motsCorriges.isEmpty()) return;

        try {
            PDPage pdPage = document.getPage(page - 1);
            Map<String, List<ExtracteurTextePdf.PositionMot>> positions =
                    ExtracteurTextePdf.positionsDesMots(document, page);

            for (String mot : motsCorriges) {
                List<ExtracteurTextePdf.PositionMot> occurrences = positions.get(mot);
                if (occurrences == null) continue;
                for (ExtracteurTextePdf.PositionMot position : occurrences) {
                    pdPage.getAnnotations().add(construireSurlignageVert(position));
                }
            }
        } catch (Exception e) {
            log.debug("Surlignage de l'aperçu impossible (page {})", page, e);
        }
    }

    /**
     * Ne retient que les mots réellement modifiés : dans « trois faute » →
     * « trois fautes », seul « fautes » mérite d'être signalé.
     */
    private List<String> motsModifies(String motFautif, String correction) {
        List<String> modifies = new ArrayList<>();
        if (correction == null || correction.isBlank()) return modifies;

        String[] avant = motFautif.trim().split("\\s+");
        String[] apres = correction.trim().split("\\s+");

        if (avant.length != apres.length) {
            modifies.addAll(List.of(apres));
            return modifies;
        }
        for (int i = 0; i < apres.length; i++) {
            if (!avant[i].equals(apres[i])) modifies.add(apres[i]);
        }
        return modifies;
    }

    private PDAnnotationHighlight construireSurlignageVert(ExtracteurTextePdf.PositionMot position) {
        PDAnnotationHighlight surlignage = new PDAnnotationHighlight();

        float bas = position.y() - position.hauteur() * 0.25f;
        float haut = position.y() + position.hauteur() * 0.85f;
        float gauche = position.x();
        float droite = position.x() + position.largeur();

        surlignage.setRectangle(new PDRectangle(gauche, bas, droite - gauche, haut - bas));
        surlignage.setQuadPoints(new float[]{
                gauche, haut, droite, haut,
                gauche, bas, droite, bas
        });
        surlignage.setColor(new PDColor(new float[]{0.30f, 0.85f, 0.42f}, PDDeviceRGB.INSTANCE));
        surlignage.setConstantOpacity(0.45f);
        surlignage.setContents("Corrigé par PrintNow");
        return surlignage;
    }

    private void apposerFiligrane(BufferedImage image) {
        Graphics2D pinceau = image.createGraphics();
        pinceau.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        pinceau.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.22f));
        pinceau.setColor(new Color(30, 41, 75));
        pinceau.setFont(new Font("SansSerif", Font.BOLD, Math.max(18, image.getWidth() / 22)));

        String texte = "APERÇU · PrintNow";
        FontMetrics mesures = pinceau.getFontMetrics();
        int largeurTexte = mesures.stringWidth(texte);

        // Répétition en diagonale sur toute la page : un recadrage ne peut pas
        // isoler une zone propre.
        AffineTransform origine = pinceau.getTransform();
        pinceau.rotate(Math.toRadians(-30), image.getWidth() / 2.0, image.getHeight() / 2.0);
        int pas = largeurTexte + 60;
        for (int y = -image.getHeight(); y < image.getHeight() * 2; y += mesures.getHeight() * 4) {
            for (int x = -image.getWidth(); x < image.getWidth() * 2; x += pas) {
                pinceau.drawString(texte, x, y);
            }
        }
        pinceau.setTransform(origine);
        pinceau.dispose();
    }
}
