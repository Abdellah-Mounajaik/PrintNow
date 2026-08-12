package com.printnow.module.user.controller;

import com.printnow.module.user.dto.JwtResponseDTO;
import com.printnow.module.user.dto.LoginRequestDTO;
import com.printnow.module.user.dto.MotDePasseOublieRequestDTO;
import com.printnow.module.user.dto.ReinitialisationRequestDTO;
import com.printnow.module.user.dto.SignupRequestDTO;
import com.printnow.module.user.dto.UserResponseDTO;
import com.printnow.module.user.model.User;
import com.printnow.module.user.repository.UserRepository;
import com.printnow.module.user.service.ReinitialisationMotDePasseService;
import com.printnow.module.user.service.UserService;

import java.util.Map;
import com.printnow.infrastructure.security.JwtUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    private final UserService userService;
    private final ReinitialisationMotDePasseService reinitialisationService;

    /**
     * Un bloc de mise au point imprimait ici le mot de passe reçu en clair, puis
     * son empreinte en base, à chaque tentative de connexion. Il refaisait au
     * passage la vérification que Spring Security effectue juste en dessous.
     *
     * Un mot de passe n'a rien à faire dans une console ou un fichier de log :
     * ceux-ci se conservent, se recopient et se partagent bien plus facilement
     * que la base de données.
     */
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequestDTO loginRequest) {
        try {
            // 1. Spring Security vérifie l'email et le mot de passe
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 2. Génération du token
            String jwt = jwtUtils.generateJwtToken(authentication);

            // 3. Récupération des infos de l'utilisateur pour la réponse
            org.springframework.security.core.userdetails.User userDetails = 
                    (org.springframework.security.core.userdetails.User) authentication.getPrincipal();
            
            User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
            String role = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .findFirst().orElse("");

            return ResponseEntity.ok(new JwtResponseDTO(jwt, user.getId(), user.getEmail(), role));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Email ou mot de passe incorrect");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequestDTO signUpRequest) {
        try {
            UserResponseDTO newUser = userService.registerClient(signUpRequest);
            return new ResponseEntity<>(newUser, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * POST /api/auth/mot-de-passe-oublie
     * Envoie un lien de réinitialisation à l'adresse indiquée.
     *
     * Répond toujours 204, que l'adresse soit connue ou non : dire le contraire
     * reviendrait à publier la liste des inscrits.
     */
    @PostMapping("/mot-de-passe-oublie")
    public ResponseEntity<Void> demanderReinitialisation(@Valid @RequestBody MotDePasseOublieRequestDTO demande) {
        reinitialisationService.demanderUnLien(demande.getEmail());
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/auth/reinitialiser?jeton=…
     * Indique si un lien est encore exploitable, pour éviter de faire saisir un
     * mot de passe avant d'annoncer que le lien a expiré.
     */
    @GetMapping("/reinitialiser")
    public ResponseEntity<Map<String, Boolean>> verifierLien(@RequestParam String jeton) {
        return ResponseEntity.ok(Map.of("valide", reinitialisationService.lienEncoreValable(jeton)));
    }

    /**
     * POST /api/auth/reinitialiser
     * Remplace le mot de passe du compte désigné par le jeton du lien.
     */
    @PostMapping("/reinitialiser")
    public ResponseEntity<Void> reinitialiser(@Valid @RequestBody ReinitialisationRequestDTO demande) {
        reinitialisationService.reinitialiser(demande);
        return ResponseEntity.noContent().build();
    }
}