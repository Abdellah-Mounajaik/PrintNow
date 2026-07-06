package com.printnow.module.etudiant.controller;

import com.printnow.module.etudiant.dto.VerificationEtudiantResponseDTO;
import com.printnow.module.etudiant.service.VerificationEtudiantService;
import com.printnow.module.user.model.User;
import com.printnow.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;

@RestController
@RequestMapping("/api/verifications-etudiants")
@RequiredArgsConstructor
public class VerificationEtudiantController {

    private final VerificationEtudiantService service;
    private final UserRepository userRepository;

    /** Client : soumet ses 2 photos */
    @PostMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VerificationEtudiantResponseDTO> soumettre(
            @RequestParam MultipartFile carteEtudiante,
            @RequestParam MultipartFile carteIdentite) {
        User user = getConnectedUser();
        return ResponseEntity.ok(service.soumettre(user, carteEtudiante, carteIdentite));
    }

    /** Client : consulte l'état de sa vérification */
    @GetMapping("/me")
    public ResponseEntity<VerificationEtudiantResponseDTO> getMaVerification() {
        User user = getConnectedUser();
        VerificationEtudiantResponseDTO dto = service.getByUserId(user.getId());
        if (dto == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(dto);
    }

    /** Admin : liste toutes les vérifications */
    @GetMapping
    public ResponseEntity<List<VerificationEtudiantResponseDTO>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    /** Admin : valide une vérification */
    @PatchMapping("/{id}/valider")
    public ResponseEntity<VerificationEtudiantResponseDTO> valider(@PathVariable Long id) {
        return ResponseEntity.ok(service.valider(id));
    }

    /** Admin : refuse une vérification */
    @PatchMapping("/{id}/refuser")
    public ResponseEntity<VerificationEtudiantResponseDTO> refuser(
            @PathVariable Long id,
            @RequestBody(required = false) java.util.Map<String, String> body) {
        String motif = body != null ? body.get("motifRefus") : null;
        return ResponseEntity.ok(service.refuser(id, motif));
    }

    /** Admin : voir une image (carte-etudiante ou carte-identite) */
    @GetMapping("/{id}/image/{type}")
    public ResponseEntity<Resource> getImage(@PathVariable Long id, @PathVariable String type) {
        return service.getImage(id, type);
    }

    private User getConnectedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non trouvé"));
    }
}
