package com.printnow.module.avis.controller;

import com.printnow.module.avis.dto.AvisImprimerieDTO;
import com.printnow.module.avis.dto.AvisRequestDTO;
import com.printnow.module.avis.dto.AvisResponseDTO;
import com.printnow.module.avis.dto.AvisTraductionDTO;
import com.printnow.module.avis.service.AvisService;
import com.printnow.module.user.model.User;
import com.printnow.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/avis")
@RequiredArgsConstructor
public class AvisController {

    private final AvisService avisService;
    private final UserRepository userRepository;

    /** Client : laisse un avis (nécessite une commande livrée). */
    @PostMapping
    public ResponseEntity<AvisResponseDTO> creerAvis(@RequestBody AvisRequestDTO request) {
        User client = getConnectedUser();
        return ResponseEntity.ok(avisService.creerAvis(client, request));
    }

    /** Public : avis d'une imprimerie. Si l'utilisateur est connecté, indique s'il peut noter. */
    @GetMapping("/imprimerie/{imprimerieId}")
    public ResponseEntity<AvisImprimerieDTO> getAvisImprimerie(@PathVariable Long imprimerieId) {
        Long clientId = getConnectedUserIdOrNull();
        return ResponseEntity.ok(avisService.getAvisImprimerie(imprimerieId, clientId));
    }

    /** Public : traduction à la demande du commentaire d'un avis ("en" ou "nl"). Rien n'est stocké. */
    @GetMapping("/{id}/traduction")
    public ResponseEntity<AvisTraductionDTO> traduireAvis(@PathVariable Long id, @RequestParam String langue) {
        return ResponseEntity.ok(avisService.traduireCommentaire(id, langue));
    }

    private User getConnectedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non trouvé"));
    }

    /** Retourne l'id du client connecté, ou null si visiteur anonyme. */
    private Long getConnectedUserIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return userRepository.findByEmail(auth.getName())
                .map(User::getId)
                .orElse(null);
    }
}
