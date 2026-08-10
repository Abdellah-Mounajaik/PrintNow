package com.printnow.module.order.controller;

import com.printnow.module.order.dto.FichierPDFResponseDTO;
import com.printnow.module.order.service.FichierPDFService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/fichiers-pdf")
@RequiredArgsConstructor
public class FichierPDFController {

    private final FichierPDFService fichierPDFService;

    /**
     * POST /api/fichiers-pdf
     * Upload un PDF lié à une ligne de commande.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FichierPDFResponseDTO> upload(
            @RequestParam MultipartFile file,
            @RequestParam Long ligneCommandeId,
            @RequestParam(defaultValue = "1") Integer nbPages) {
        return ResponseEntity.ok(fichierPDFService.uploadFichier(ligneCommandeId, file, nbPages));
    }

    /**
     * POST /api/fichiers-pdf/verifier-format
     * Contrôle qu'un PDF correspond au format d'un produit, avant paiement et
     * sans rien enregistrer. Le même contrôle a lieu à l'envoi du fichier, mais
     * trop tard : la commande serait déjà réglée.
     */
    @PostMapping(value = "/verifier-format", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> verifierFormat(@RequestParam MultipartFile file,
                                               @RequestParam Long produitId) {
        fichierPDFService.verifierFormat(file, produitId);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/fichiers-pdf/{id}/download
     * Télécharge / affiche un PDF dans le navigateur.
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        return fichierPDFService.downloadFichier(id);
    }
}
