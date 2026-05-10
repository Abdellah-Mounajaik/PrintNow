package com.printnow.module.order.controller;

import com.printnow.module.order.dto.CommandeRequestDTO;
import com.printnow.module.order.dto.CommandeResponseDTO;
import com.printnow.module.order.service.CommandeService;
import com.printnow.module.user.model.User;
import com.printnow.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/commandes")
@RequiredArgsConstructor
public class CommandeController {

    private final CommandeService commandeService;
    private final UserRepository userRepository;

    /**
     * POST /api/commandes
     * Permet à un client connecté de passer une nouvelle commande.
     */
    @PostMapping
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
}