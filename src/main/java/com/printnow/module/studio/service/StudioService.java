package com.printnow.module.studio.service;

import com.printnow.infrastructure.security.UtilisateurCourant;
import com.printnow.module.studio.dto.GenerationResponseDTO;
import com.printnow.module.studio.dto.GenererRequestDTO;
import com.printnow.module.studio.enums.StatutGeneration;
import com.printnow.module.studio.enums.TypeSupport;
import com.printnow.module.studio.model.GenerationSupport;
import com.printnow.module.studio.model.PropositionSupport;
import com.printnow.module.studio.repository.GenerationSupportRepository;
import com.printnow.module.studio.repository.PropositionSupportRepository;
import com.printnow.module.studio.service.gabarit.Gabarit;
import com.printnow.module.studio.service.gabarit.Style;
import com.printnow.module.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Orchestration de la génération de supports.
 *
 * Enchaîne : validation → appel IA (avec le prompt du gabarit) → rendu PDF →
 * aperçu PNG → stockage hors zone publique. Le service ne connaît aucun type en
 * particulier : il choisit le {@link Gabarit} correspondant et le laisse faire.
 * Synchrone et à une proposition pour l'instant ; les « 3 propositions »
 * viendront ensuite.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StudioService {

    private static final int BRIEF_MAX = 5000;

    private final GenerationSupportRepository generationRepository;
    private final PropositionSupportRepository propositionRepository;
    private final StudioIaService iaService;
    private final StyleIaService styleService;
    private final List<Gabarit> gabarits;
    private final UtilisateurCourant utilisateurCourant;

    @Value("${studio.stockage.dir:studio}")
    private String stockageDir;

    @Transactional
    public GenerationResponseDTO generer(GenererRequestDTO requete) {
        TypeSupport type = lireType(requete.getType());
        Gabarit gabarit = maquettePour(type);

        String brief = requete.getBrief() == null ? "" : requete.getBrief().trim();
        if (brief.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La description est obligatoire.");
        }
        if (brief.length() > BRIEF_MAX) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La description est trop longue (max " + BRIEF_MAX + " caractères).");
        }

        User client = utilisateurCourant.utilisateur();
        GenerationSupport generation = generationRepository.save(GenerationSupport.builder()
                .client(client)
                .typeSupport(type)
                .brief(brief)
                .statut(StatutGeneration.EN_COURS)
                .suivi(UUID.randomUUID().toString())
                .build());

        try {
            // Le contenu est généré une fois (commun aux propositions). Chaque
            // proposition reçoit ensuite une structure et une palette différentes,
            // tirées au hasard dans le pool du type.
            String contenuJson = iaService.genererJson(gabarit.promptSysteme(), brief);
            List<Style> styles = styleService.troisStyles(brief);
            List<String> structures = troisStructures(gabarit.structures());

            for (int i = 0; i < styles.size(); i++) {
                String structure = structures.get(i);
                Style style = styles.get(i);
                byte[] pdf = gabarit.rendre(contenuJson, style, structure);   // parse + valide + rend
                String base = UUID.randomUUID().toString();
                Path cheminPdf = ecrire(base + ".pdf", pdf);
                Path cheminApercu = ecrire(base + ".png", apercu(pdf));

                PropositionSupport proposition = propositionRepository.save(PropositionSupport.builder()
                        .generation(generation)
                        .gabaritCode(structure)
                        .paletteCode(style.code())
                        .policeCode(style.police().code())
                        .contenuJson(contenuJson)
                        .cheminPdf(cheminPdf.toString())
                        .cheminApercu(cheminApercu.toString())
                        .choisie(false)
                        .build());
                generation.getPropositions().add(proposition);
            }

            generation.setStatut(StatutGeneration.PRETE);
        } catch (StudioIaService.GenerationIndisponible e) {
            generation.setStatut(StatutGeneration.ECHOUEE);
            log.warn("Génération {} échouée (IA indisponible) : {}", generation.getId(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "La génération n'a pas pu aboutir, réessayez dans un instant.", e);
        } catch (Exception e) {
            generation.setStatut(StatutGeneration.ECHOUEE);
            log.warn("Génération {} échouée : {}", generation.getId(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Le support n'a pas pu être fabriqué.", e);
        }

        return versDto(generationRepository.save(generation));
    }

    @Transactional(readOnly = true)
    public GenerationResponseDTO getGeneration(Long id) {
        return versDto(chargerAMoi(id));
    }

    /** Charge une proposition en s'assurant qu'elle appartient au client courant. */
    @Transactional(readOnly = true)
    public PropositionSupport chargerPropositionAMoi(Long propositionId) {
        PropositionSupport proposition = propositionRepository.findById(propositionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proposition introuvable."));
        refuserSiPasAMoi(proposition.getGeneration());
        return proposition;
    }

    // --- interne ---------------------------------------------------------------

    /** L'unique gabarit qui couvre un type. */
    private Gabarit maquettePour(TypeSupport type) {
        return gabarits.stream()
                .filter(g -> g.type() == type)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED,
                        "Ce type de support n'est pas encore disponible."));
    }

    /** Trois structures distinctes tirées au hasard du pool (jamais les mêmes d'une fois à l'autre). */
    private List<String> troisStructures(List<String> pool) {
        List<String> melange = new ArrayList<>(pool);
        Collections.shuffle(melange);
        List<String> choix = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            choix.add(melange.get(i % melange.size()));
        }
        return choix;
    }

    private GenerationSupport chargerAMoi(Long id) {
        GenerationSupport generation = generationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Génération introuvable."));
        refuserSiPasAMoi(generation);
        return generation;
    }

    private void refuserSiPasAMoi(GenerationSupport generation) {
        if (generation.getClient() == null
                || !generation.getClient().getId().equals(utilisateurCourant.id())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cette génération ne vous appartient pas.");
        }
    }

    private TypeSupport lireType(String type) {
        try {
            return TypeSupport.valueOf(type == null ? "" : type.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Type de support inconnu : " + type);
        }
    }

    private byte[] apercu(byte[] pdf) throws Exception {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            BufferedImage image = new PDFRenderer(document).renderImageWithDPI(0, 110, ImageType.RGB);
            apposerFiligrane(image);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        }
    }

    /**
     * Filigrane « APERÇU · PrintNow » répété en diagonale, comme l'aperçu de la
     * correction : le client juge le rendu sans obtenir d'image exploitable. Le
     * PDF, lui, reste propre — il ne rejoint la commande qu'une fois choisi et payé.
     */
    private void apposerFiligrane(BufferedImage image) {
        // La couleur dépend de la clarté du fond : du bleu marine sur un thème
        // clair, du blanc cassé sur un thème sombre — sinon, foncé sur foncé, le
        // filigrane disparaît (cas d'un CV en thème sombre).
        boolean fondSombre = fondEstSombre(image);
        Color couleur = fondSombre ? new Color(236, 239, 245) : new Color(30, 41, 75);

        Graphics2D pinceau = image.createGraphics();
        pinceau.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        pinceau.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fondSombre ? 0.24f : 0.20f));
        pinceau.setColor(couleur);
        pinceau.setFont(new Font("SansSerif", Font.BOLD, Math.max(16, image.getWidth() / 24)));

        String texte = "APERÇU · PrintNow";
        FontMetrics mesures = pinceau.getFontMetrics();
        int largeurTexte = mesures.stringWidth(texte);

        // Répété en diagonale sur toute la page : un recadrage ne peut pas isoler
        // une zone propre.
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

    /** Vrai si la page rendue est majoritairement sombre (luminance perçue moyenne, sur un échantillon). */
    private boolean fondEstSombre(BufferedImage image) {
        int pasX = Math.max(1, image.getWidth() / 40);
        int pasY = Math.max(1, image.getHeight() / 40);
        long somme = 0;
        int n = 0;
        for (int y = 0; y < image.getHeight(); y += pasY) {
            for (int x = 0; x < image.getWidth(); x += pasX) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
                somme += Math.round(0.299 * r + 0.587 * g + 0.114 * b);
                n++;
            }
        }
        return n > 0 && (double) somme / n < 128;
    }

    private Path ecrire(String nom, byte[] contenu) throws Exception {
        Path dir = Path.of(stockageDir);
        Files.createDirectories(dir);
        Path chemin = dir.resolve(nom);
        Files.write(chemin, contenu);
        return chemin.toAbsolutePath();
    }

    private GenerationResponseDTO versDto(GenerationSupport g) {
        List<GenerationResponseDTO.PropositionResponseDTO> props = g.getPropositions().stream()
                .map(p -> new GenerationResponseDTO.PropositionResponseDTO(
                        p.getId(), p.getGabaritCode(), p.getPaletteCode(),
                        p.getCheminApercu() != null, p.getCheminPdf() != null))
                .toList();
        return new GenerationResponseDTO(g.getId(), g.getTypeSupport().name(),
                g.getStatut().name(), g.getSuivi(), props);
    }
}
