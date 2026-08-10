package com.printnow.module.order.controller;


import com.printnow.module.order.dto.CommandeRequestDTO;
import com.printnow.module.order.dto.CommandeResponseDTO;
import com.printnow.module.order.service.CommandeService;
import com.printnow.module.order.service.FactureCommissionService;
import com.printnow.module.order.service.FactureService;
import com.printnow.module.order.service.ReleveVenteService;
import com.printnow.module.payment.service.RemboursementService;
import com.printnow.module.user.model.User;
import com.printnow.module.user.repository.UserRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
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
    private final FactureCommissionService factureCommissionService;
    private final ReleveVenteService releveVenteService;
    private final RemboursementService remboursementService;
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

        // 3. Le paiement est confirmé côté navigateur avant l'appel, mais on le
        // revérifie directement auprès de Stripe (jamais confiance au seul client).
        boolean paiementConfirme = false;
        Long montantRegle = null;
        if (request.getPaymentIntentId() != null && !request.getPaymentIntentId().isBlank()) {
            try {
                PaymentIntent intent = PaymentIntent.retrieve(request.getPaymentIntentId());
                if (!"succeeded".equals(intent.getStatus())) {
                    return ResponseEntity.badRequest().build();
                }
                paiementConfirme = true;
                montantRegle = intent.getAmount();
            } catch (StripeException e) {
                return ResponseEntity.badRequest().build();
            }
        }

        // 4. La commande et les vérifications orthographiques réglées avec elle
        // sont enregistrées d'un seul tenant : si le paiement ne couvre pas les
        // corrections demandées, rien n'est conservé.
        try {
            return ResponseEntity.ok(
                    commandeService.passerCommande(request, client, paiementConfirme, montantRegle));

        } catch (RuntimeException e) {
            // La commande est refusée alors que Stripe a déjà encaissé : sans
            // remboursement, le client serait débité sans rien recevoir.
            if (paiementConfirme) {
                remboursementService.rembourser(request.getPaymentIntentId(), e.getMessage());
            }
            throw e;
        }
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

    /**
     * GET /api/commandes/{id}/facture-commission
     * Télécharge la facture de commission PDF d'une commande — ce que PrintNow
     * a gagné sur cette vente. Réservée à l'admin.
     */
    @GetMapping("/{id}/facture-commission")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> telechargerFactureCommission(@PathVariable Long id) {
        byte[] pdf = factureCommissionService.genererFactureCommission(id);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"commission-" + id + ".pdf\"")
                .body(pdf);
    }

    /**
     * GET /api/commandes/{id}/releve-vente
     * Télécharge le relevé de vente PDF d'une commande — ce que l'imprimerie a
     * réellement perçu une fois la commission PrintNow déduite. Accessible à
     * l'admin (n'importe quelle commande) ou à l'imprimeur propriétaire de la
     * commande concernée.
     */
    @GetMapping("/{id}/releve-vente")
    @PreAuthorize("hasRole('ADMIN') or hasRole('IMPRIMERIE')")
    public ResponseEntity<byte[]> telechargerReleveVente(@PathVariable Long id) {
        boolean estAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        Long gerantId = null;
        if (!estAdmin) {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            User imprimeur = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'email : " + email));
            gerantId = imprimeur.getId();
        }

        byte[] pdf = releveVenteService.genererReleveVente(id, gerantId);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"vente-" + id + ".pdf\"")
                .body(pdf);
    }
}