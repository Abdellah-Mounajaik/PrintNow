package com.printnow.module.order.controller;

import com.printnow.module.order.dto.LivraisonSuiviDTO;
import com.printnow.module.order.service.LivraisonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/livraisons")
@RequiredArgsConstructor
public class LivraisonController {

    private final LivraisonService livraisonService;

    /**
     * Imprimeur : saisit le numéro de suivi bpost après dépôt du colis.
     * PATCH /api/livraisons/{commandeId}/suivi
     * Body : { "numeroSuivi": "010123456789" }
     */
    @PatchMapping("/{commandeId}/suivi")
    public ResponseEntity<LivraisonSuiviDTO> ajouterNumeroSuivi(
            @PathVariable Long commandeId,
            @RequestBody Map<String, String> body) {
        String numeroSuivi = body.get("numeroSuivi");
        if (numeroSuivi == null || numeroSuivi.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(livraisonService.ajouterNumeroSuivi(commandeId, numeroSuivi));
    }

    /**
     * Client / imprimeur : récupère le statut en temps réel via AfterShipping.
     * GET /api/livraisons/{commandeId}/suivi
     */
    @GetMapping("/{commandeId}/suivi")
    public ResponseEntity<LivraisonSuiviDTO> getStatutLivraison(@PathVariable Long commandeId) {
        return ResponseEntity.ok(livraisonService.getStatutLivraison(commandeId));
    }
}
