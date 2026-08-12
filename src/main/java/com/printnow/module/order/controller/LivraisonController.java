package com.printnow.module.order.controller;

import com.printnow.module.order.dto.LivraisonSuiviDTO;
import com.printnow.module.order.service.DroitsCommandeService;
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
    private final DroitsCommandeService droits;

    /**
     * Imprimeur : saisit le numéro de suivi bpost après dépôt du colis.
     * PATCH /api/livraisons/{commandeId}/suivi
     * Body : { "numeroSuivi": "010123456789" }
     *
     * Réservé à celui qui expédie : un faux numéro de suivi égare le client
     * autant qu'un colis perdu.
     */
    @PatchMapping("/{commandeId}/suivi")
    public ResponseEntity<LivraisonSuiviDTO> ajouterNumeroSuivi(
            @PathVariable Long commandeId,
            @RequestBody Map<String, String> body) {
        String numeroSuivi = body.get("numeroSuivi");
        if (numeroSuivi == null || numeroSuivi.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        droits.verifierGestionCommande(commandeId);
        return ResponseEntity.ok(livraisonService.ajouterNumeroSuivi(commandeId, numeroSuivi));
    }

    /**
     * Client / imprimeur : récupère le statut en temps réel via AfterShipping.
     * GET /api/livraisons/{commandeId}/suivi
     */
    @GetMapping("/{commandeId}/suivi")
    public ResponseEntity<LivraisonSuiviDTO> getStatutLivraison(@PathVariable Long commandeId) {
        droits.verifierAccesCommande(commandeId);
        return ResponseEntity.ok(livraisonService.getStatutLivraison(commandeId));
    }
}
