package com.printnow.module.studio.controller;

import com.printnow.module.studio.dto.GenerationResponseDTO;
import com.printnow.module.studio.dto.GenererRequestDTO;
import com.printnow.module.studio.model.PropositionSupport;
import com.printnow.module.studio.service.StudioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Génération de supports par IA (tranche verticale : CV).
 *
 * Réservé aux clients — imprimeur/admin ne commandent pas. Les PDF et aperçus ne
 * sont jamais servis en statique : ils passent par ces endpoints authentifiés,
 * qui vérifient que la proposition appartient bien au demandeur (leçon IDOR).
 */
@RestController
@RequestMapping("/api/studio")
@RequiredArgsConstructor
public class StudioController {

    private final StudioService studioService;

    @PostMapping("/generer")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<GenerationResponseDTO> generer(@RequestBody GenererRequestDTO requete) {
        return ResponseEntity.ok(studioService.generer(requete));
    }

    @GetMapping("/generations/{id}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<GenerationResponseDTO> getGeneration(@PathVariable Long id) {
        return ResponseEntity.ok(studioService.getGeneration(id));
    }

    @GetMapping("/propositions/{id}/apercu")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<byte[]> apercu(@PathVariable Long id) {
        PropositionSupport p = studioService.chargerPropositionAMoi(id);
        return fichier(p.getCheminApercu(), MediaType.IMAGE_PNG, "apercu-" + id + ".png", false);
    }

    @GetMapping("/propositions/{id}/pdf")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        PropositionSupport p = studioService.chargerPropositionAMoi(id);
        return fichier(p.getCheminPdf(), MediaType.APPLICATION_PDF, "support-" + id + ".pdf", true);
    }

    private ResponseEntity<byte[]> fichier(String chemin, MediaType type, String nom, boolean telecharger) {
        if (chemin == null) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Fichier indisponible.");
        }
        try {
            byte[] contenu = Files.readAllBytes(Path.of(chemin));
            String disposition = (telecharger ? "attachment" : "inline") + "; filename=\"" + nom + "\"";
            return ResponseEntity.ok()
                    .contentType(type)
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                    .body(contenu);
        } catch (Exception e) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Fichier introuvable.");
        }
    }
}
