package com.printnow.module.correction.controller;

import com.printnow.module.correction.dto.DemandeCorrectionDTO;
import com.printnow.module.correction.dto.FauteDTO;
import com.printnow.module.correction.dto.VerificationResponseDTO;
import com.printnow.module.correction.model.VerificationOrthographe;
import com.printnow.module.correction.repository.VerificationOrthographeRepository;
import com.printnow.module.correction.service.ApercuCorrectionService;
import com.printnow.module.correction.service.CorrectionService;
import com.printnow.module.correction.service.ProgressionAnalyse;
import com.printnow.module.user.model.User;
import com.printnow.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/corrections")
@RequiredArgsConstructor
@Slf4j
public class CorrectionController {

    private final CorrectionService correctionService;
    private final ProgressionAnalyse progressionAnalyse;
    private final ApercuCorrectionService apercuService;
    private final VerificationOrthographeRepository repository;
    private final UserRepository userRepository;

    /**
     * POST /api/corrections/analyser
     * Analyse gratuite : le client voit les fautes et les corrections proposées
     * afin de décider en connaissance de cause. Seule la production du PDF
     * corrigé est facturée, au moment du paiement de la commande.
     */
    @PostMapping(value = "/analyser", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<VerificationResponseDTO> analyser(
            @RequestParam("file") MultipartFile fichier,
            @RequestParam(value = "suivi", required = false) String suivi) {
        VerificationOrthographe verification = correctionService.analyser(fichier, utilisateurConnecte(), suivi);
        return ResponseEntity.ok(versDTO(verification, correctionService.fautesDe(verification)));
    }

    /**
     * GET /api/corrections/progression/{suivi}
     * Avancement d'une analyse en cours, interrogé pendant que le navigateur
     * attend la réponse. L'identifiant est celui fourni à l'envoi.
     */
    @GetMapping("/progression/{suivi}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<Map<String, Object>> progression(@PathVariable String suivi) {
        ProgressionAnalyse.Etape etape = progressionAnalyse.lire(suivi);
        if (etape == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(Map.of(
                "pourcentage", etape.pourcentage(),
                "libelle", etape.libelle()));
    }

    /**
     * GET /api/corrections/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<VerificationResponseDTO> consulter(@PathVariable Long id) {
        VerificationOrthographe verification = chargerSiProprietaire(id);
        return ResponseEntity.ok(versDTO(verification, correctionService.fautesDe(verification)));
    }

    /**
     * GET /api/corrections/{id}/apercu
     * Image filigranée de la page corrigée, consultable avant paiement.
     * Le PDF lui-même n'est jamais exposé ici : seule une image l'est.
     */
    @PostMapping(value = "/{id}/apercu", produces = MediaType.IMAGE_PNG_VALUE)
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<byte[]> apercu(@PathVariable Long id,
                                         @RequestParam(required = false) Integer page,
                                         @RequestBody(required = false) DemandeCorrectionDTO choixClient) {
        VerificationOrthographe verification = chargerSiProprietaire(id);
        int pageDemandee = page != null ? page : apercuService.premierePageAvecFaute(verification);

        List<Integer> ignorees = choixClient == null ? null : choixClient.getFautesIgnorees();
        Map<Integer, String> remplacements = choixClient == null ? null : choixClient.getRemplacementsChoisis();

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                // L'aperçu ne doit pas rester en cache : le document est personnel.
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(apercuService.genererApercu(verification, pageDemandee, ignorees, remplacements));
    }

    /**
     * GET /api/corrections/{id}/pdf
     * Télécharge le PDF corrigé (réservé aux vérifications payées).
     */
    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<Resource> telechargerPdf(@PathVariable Long id) {
        VerificationOrthographe verification = chargerSiProprietaire(id);

        if (!Boolean.TRUE.equals(verification.getPayee()) || verification.getCheminCorrige() == null) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED,
                    "Le PDF corrigé n'est disponible qu'après paiement.");
        }

        Path chemin = Paths.get(verification.getCheminCorrige());
        if (!Files.exists(chemin)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "PDF corrigé introuvable.");
        }

        String nom = verification.getNomFichier() == null
                ? "document-corrige.pdf"
                : verification.getNomFichier().replaceFirst("\\.pdf$", "") + "-corrige.pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nom + "\"")
                .body(new FileSystemResource(chemin));
    }

    // ─── Utilitaires ──────────────────────────────────────────────────────────

    private User utilisateurConnecte() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur introuvable."));
    }

    /**
     * Charge la vérification en s'assurant qu'elle appartient bien à l'utilisateur
     * connecté : les PDF déposés sont des documents personnels.
     */
    private VerificationOrthographe chargerSiProprietaire(Long id) {
        VerificationOrthographe verification = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vérification introuvable."));

        if (!verification.getClient().getId().equals(utilisateurConnecte().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé.");
        }
        return verification;
    }

    private VerificationResponseDTO versDTO(VerificationOrthographe verification, List<FauteDTO> fautes) {
        return new VerificationResponseDTO(
                verification.getId(),
                verification.getNomFichier(),
                verification.getNbPages(),
                verification.getNbFautes(),
                verification.getPrix(),
                verification.getPayee(),
                verification.getNbCorrigees(),
                fautes
        );
    }
}
