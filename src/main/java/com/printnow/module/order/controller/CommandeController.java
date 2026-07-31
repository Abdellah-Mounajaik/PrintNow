package com.printnow.module.order.controller;

import com.printnow.module.order.dto.CommandeRequestDTO;
import com.printnow.module.order.dto.CommandeResponseDTO;
import com.printnow.module.order.service.CommandeService;
import com.printnow.module.order.service.FactureService;
import com.printnow.module.user.model.User;
import com.printnow.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/commandes")
@RequiredArgsConstructor
public class CommandeController {

    private final CommandeService commandeService;
    private final FactureService factureService;
    private final UserRepository userRepository;

    /**
     * POST /api/commandes
     * Permet à un client connecté de passer une nouvelle commande.
     */
    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<CommandeResponseDTO> passerCommande(@RequestBody CommandeRequestDTO request) {
        // 1. On récupère l'email de l'utilisateur actuellement connecté via le Token JWT
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        
        // 2. On cherche cet utilisateur dans la base de données
        User client = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'email : " + email));
        
        // 3. On crée la commande via le service et on retourne le DTO généré
        CommandeResponseDTO nouvelleCommande = commandeService.createCommande(request, client);
        
        return ResponseEntity.ok(nouvelleCommande);
    }

    /**
     * GET /api/commandes/imprimerie/{id}
     * Permet de récupérer toutes les commandes d'une imprimerie spécifique (pour le Dashboard Pro).
     */
    @GetMapping("/imprimerie/{id}")
    public ResponseEntity<List<CommandeResponseDTO>> getCommandesImprimeur(@PathVariable Long id) {
        List<CommandeResponseDTO> commandes = commandeService.getCommandesForImprimerie(id);
        return ResponseEntity.ok(commandes);
    }

    /**
     * PATCH /api/commandes/{id}/statut
     * Permet à l'imprimeur de faire avancer le statut d'une commande.
     */
    @PatchMapping("/{id}/statut")
    public ResponseEntity<CommandeResponseDTO> updateStatut(
            @PathVariable Long id,
            @RequestParam String statut) {
        return ResponseEntity.ok(commandeService.updateStatut(id, statut));
    }

    /**
     * GET /api/commandes
     * Récupère toutes les commandes (Dashboard Admin).
     */
    @GetMapping
    public ResponseEntity<List<CommandeResponseDTO>> getAllCommandes() {
        return ResponseEntity.ok(commandeService.getAllCommandes());
    }

    /**
     * GET /api/commandes/me
     * Récupère les commandes du client connecté (Dashboard Client).
     */
    @GetMapping("/me")
    public ResponseEntity<List<CommandeResponseDTO>> getMesCommandes() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User client = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'email : " + email));
        List<CommandeResponseDTO> commandes = commandeService.getCommandesForClient(client.getId());
        return ResponseEntity.ok(commandes);
    }

    /**
     * GET /api/commandes/{id}/facture
     * Télécharge la facture PDF d'une commande du client connecté (générée à
     * la demande, uniquement s'il en est bien le propriétaire et qu'elle est payée).
     */
    @GetMapping("/{id}/facture")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<byte[]> telechargerFacture(@PathVariable Long id) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User client = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'email : " + email));

        byte[] pdf = factureService.genererFacture(id, client.getId());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"facture-" + id + ".pdf\"")
                .body(pdf);
    }
}