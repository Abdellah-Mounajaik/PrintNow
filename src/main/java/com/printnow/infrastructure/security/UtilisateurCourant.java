package com.printnow.infrastructure.security;

import com.printnow.module.user.model.User;
import com.printnow.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Répond à « qui demande ? » pour les services qui contrôlent des droits.
 *
 * Chaque service de droits refaisait sa propre lecture du contexte de sécurité.
 * Les mettre en commun évite surtout qu'une de ces copies dérive : une seule
 * ligne décide ici de ce qu'est un administrateur.
 */
@Component
@RequiredArgsConstructor
public class UtilisateurCourant {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final UserRepository userRepository;

    public boolean estAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(ROLE_ADMIN::equals);
    }

    /** @throws ResponseStatusException 401 si personne n'est authentifié */
    public User utilisateur() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentification requise.");

        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Compte introuvable."));
    }

    public Long id() {
        return utilisateur().getId();
    }
}
