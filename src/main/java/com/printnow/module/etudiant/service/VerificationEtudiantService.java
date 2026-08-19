package com.printnow.module.etudiant.service;

import com.printnow.infrastructure.email.EmailService;
import com.printnow.module.etudiant.dto.VerificationEtudiantResponseDTO;
import com.printnow.module.etudiant.enums.StatutEtudiant;
import com.printnow.module.etudiant.mapper.VerificationEtudiantMapper;
import com.printnow.module.etudiant.model.VerificationEtudiant;
import com.printnow.module.etudiant.repository.VerificationEtudiantRepository;
import com.printnow.module.etudiant.service.VerificationEtudiantIaService.Resultat;
import com.printnow.module.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationEtudiantService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private final VerificationEtudiantRepository repository;
    private final VerificationEtudiantMapper mapper;
    private final EmailService emailService;
    private final VerificationEtudiantIaService iaService;

    /**
     * Au-delà de ce nombre de refus d'affilée, les resoumissions sont bloquées :
     * sans limite, un compte refusé pouvait retenter indéfiniment (spam de
     * demandes, et donc d'appels à l'IA qui les analyse).
     */
    private static final int MAX_TENTATIVES = 3;

    @Transactional
    public VerificationEtudiantResponseDTO soumettre(User user, MultipartFile carteEtudiante, MultipartFile carteIdentite) {
        VerificationEtudiant verification = repository.findByUser_Id(user.getId())
                .orElseGet(VerificationEtudiant::new);

        if (verification.getId() != null) {
            if (verification.getStatut() == StatutEtudiant.ACCEPTE && estValide(verification)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Votre vérification est déjà acceptée et en cours de validité.");
            }
            if (verification.getStatut() == StatutEtudiant.EN_ATTENTE) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Votre demande est déjà en attente de vérification.");
            }
            if (verification.getStatut() == StatutEtudiant.REFUSE && verification.getNombreRefus() >= MAX_TENTATIVES) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Vous avez atteint le nombre maximum de tentatives (" + MAX_TENTATIVES
                                + "). Contactez le support via la page Contact pour une vérification manuelle.");
            }
        }

        // Une nouvelle soumission remplace la précédente : on efface d'abord ses
        // images, sinon elles restent sur le disque sans plus servir à rien.
        effacerLesJustificatifs(verification);

        try {
            Path dir = Paths.get(uploadDir, "verifications", user.getId().toString());
            Files.createDirectories(dir);

            String etudianteName = System.currentTimeMillis() + "_carte_etudiante_" + carteEtudiante.getOriginalFilename();
            String identiteName = System.currentTimeMillis() + "_carte_identite_" + carteIdentite.getOriginalFilename();

            Files.copy(carteEtudiante.getInputStream(), dir.resolve(etudianteName), StandardCopyOption.REPLACE_EXISTING);
            Files.copy(carteIdentite.getInputStream(), dir.resolve(identiteName), StandardCopyOption.REPLACE_EXISTING);

            verification.setUser(user);
            verification.setStatut(StatutEtudiant.EN_ATTENTE);
            verification.setCarteEtudiantePath(dir.resolve(etudianteName).toAbsolutePath().toString());
            verification.setCarteIdentitePath(dir.resolve(identiteName).toAbsolutePath().toString());
            verification.setDateSoumission(LocalDateTime.now());
            verification.setDateValidation(null);
            verification.setValableJusquA(null);
            verification.setMotifRefus(null);
            // Une resoumission (après un refus) ne doit pas garder le verdict de
            // l'analyse précédente : chaque dépôt est réanalysé à neuf.
            verification.setVerdictIa(null);
            verification.setNomExtraitCarteEtudiante(null);
            verification.setNomExtraitCarteIdentite(null);
            verification.setDecisionAutomatique(false);

            VerificationEtudiant saved = repository.save(verification);
            analyserEtDeciderAutomatiquement(saved, carteEtudiante, carteIdentite);

            return mapper.toDto(saved);
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de l'upload des fichiers", e);
        }
    }

    /**
     * Analyse les deux documents et, selon le verdict, décide seule
     * (correspondance claire → acceptation, incohérence manifeste → refus) ou
     * laisse la demande EN_ATTENTE pour l'admin (doute, ou IA indisponible).
     *
     * Ne relance rien en cas d'échec : une analyse ratée équivaut simplement à
     * l'absence d'IA, la demande suit alors le circuit manuel habituel.
     */
    private void analyserEtDeciderAutomatiquement(VerificationEtudiant v, MultipartFile carteEtudiante, MultipartFile carteIdentite) {
        Resultat resultat;
        try {
            resultat = iaService.analyser(
                    carteEtudiante.getBytes(), carteEtudiante.getContentType(),
                    carteIdentite.getBytes(), carteIdentite.getContentType());
        } catch (IOException e) {
            log.warn("Lecture des fichiers impossible pour l'analyse automatique de la vérification {}", v.getId(), e);
            return;
        }
        if (resultat == null || resultat.getVerdict() == null) return;

        v.setVerdictIa(resultat.getVerdict());
        v.setNomExtraitCarteEtudiante(resultat.getNomCarteEtudiante());
        v.setNomExtraitCarteIdentite(resultat.getNomCarteIdentite());

        switch (resultat.getVerdict()) {
            case CORRESPONDANCE -> appliquerAcceptation(v, true);
            case DIVERGENCE_MANIFESTE -> appliquerRefus(v,
                    "Les documents fournis ne correspondent pas.", true);
            case INCERTAIN -> repository.save(v); // reste EN_ATTENTE, verdict et noms conservés pour l'admin
        }
    }

    @Transactional
    public VerificationEtudiantResponseDTO getByUserId(Long userId) {
        return repository.findByUser_Id(userId)
                .map(v -> {
                    checkAndExpire(v);
                    return mapper.toDto(v);
                })
                .orElse(null);
    }

    public List<VerificationEtudiantResponseDTO> getAll() {
        return repository.findAllByOrderByDateSoumissionDesc()
                .stream()
                .map(v -> {
                    checkAndExpire(v);
                    return mapper.toDto(v);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public VerificationEtudiantResponseDTO valider(Long id) {
        VerificationEtudiant v = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vérification introuvable"));
        appliquerAcceptation(v, false);
        return mapper.toDto(v);
    }

    @Transactional
    public VerificationEtudiantResponseDTO refuser(Long id, String motifRefus) {
        if (motifRefus == null || motifRefus.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le motif de refus est obligatoire.");
        }
        VerificationEtudiant v = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vérification introuvable"));
        appliquerRefus(v, motifRefus, false);
        return mapper.toDto(v);
    }

    /** @param automatique true si décidé par l'analyse IA, sans intervention d'un admin. */
    private void appliquerAcceptation(VerificationEtudiant v, boolean automatique) {
        v.setStatut(StatutEtudiant.ACCEPTE);
        v.setDateValidation(LocalDateTime.now());
        v.setValableJusquA(calculerExpiration());
        v.setDecisionAutomatique(automatique);
        // Une acceptation solde les refus précédents : ils ne doivent pas peser
        // sur un futur renouvellement (année suivante) qui n'a rien à voir.
        v.setNombreRefus(0);
        // La décision prise, les pièces ont rempli leur rôle : on ne garde que le verdict.
        effacerLesJustificatifs(v);
        repository.save(v);

        emailService.envoyerVerificationAcceptee(v.getUser().getEmail(), v.getUser().getPrenom());
    }

    /** @param automatique true si décidé par l'analyse IA, sans intervention d'un admin. */
    private void appliquerRefus(VerificationEtudiant v, String motifRefus, boolean automatique) {
        v.setStatut(StatutEtudiant.REFUSE);
        v.setDateValidation(LocalDateTime.now());
        v.setMotifRefus(motifRefus);
        v.setDecisionAutomatique(automatique);
        v.setNombreRefus(v.getNombreRefus() + 1);
        // Refus aussi définitif qu'une acceptation : les pièces n'ont plus lieu d'être.
        effacerLesJustificatifs(v);
        repository.save(v);

        emailService.envoyerVerificationRefusee(v.getUser().getEmail(), v.getUser().getPrenom(), motifRefus);
    }

    public ResponseEntity<Resource> getImage(Long id, String type) {
        VerificationEtudiant v = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vérification introuvable"));
        String path = type.equals("etudiante") ? v.getCarteEtudiantePath() : v.getCarteIdentitePath();
        if (path == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image introuvable");
        try {
            Path filePath = Paths.get(path);
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fichier introuvable");
            String contentType = Files.probeContentType(filePath);
            MediaType mediaType = contentType != null ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM;
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filePath.getFileName() + "\"")
                    .body(resource);
        } catch (IOException e) {
            throw new RuntimeException("Erreur lecture image", e);
        }
    }

    /**
     * Efface les deux pièces justificatives et coupe les références en base.
     *
     * Ces images ne servent qu'à comparer la carte étudiante à la carte
     * d'identité au moment de la vérification. La décision rendue — ou une
     * nouvelle soumission déposée — leur finalité est remplie et le RGPD impose
     * de les effacer (art. 5.1.e), d'autant qu'une carte d'identité porte le
     * numéro de registre national, spécialement protégé en Belgique. On ne
     * conserve que le verdict : statut, date et validité.
     */
    private void effacerLesJustificatifs(VerificationEtudiant v) {
        effacerDuDisque(v.getCarteEtudiantePath());
        effacerDuDisque(v.getCarteIdentitePath());
        v.setCarteEtudiantePath(null);
        v.setCarteIdentitePath(null);
    }

    private void effacerDuDisque(String chemin) {
        if (chemin == null || chemin.isBlank()) return;
        try {
            Files.deleteIfExists(Paths.get(chemin));
        } catch (IOException e) {
            // Un fichier déjà absent ou verrouillé ne doit pas bloquer la décision de l'admin.
            log.warn("Justificatif étudiant impossible à effacer ({}) : {}", chemin, e.getMessage());
        }
    }

    private void checkAndExpire(VerificationEtudiant v) {
        if (v.getStatut() == StatutEtudiant.ACCEPTE && v.getValableJusquA() != null
                && LocalDateTime.now().isAfter(v.getValableJusquA())) {
            v.setStatut(StatutEtudiant.EXPIRE);
            repository.save(v);
        }
    }

    private boolean estValide(VerificationEtudiant v) {
        return v.getValableJusquA() != null && LocalDateTime.now().isBefore(v.getValableJusquA());
    }

    private LocalDateTime calculerExpiration() {
        LocalDate today = LocalDate.now();
        LocalDate june30 = LocalDate.of(today.getYear(), 6, 30);
        return (today.isAfter(june30) ? june30.plusYears(1) : june30).atTime(23, 59, 59);
    }

}
