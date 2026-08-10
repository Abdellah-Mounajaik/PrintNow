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
import org.springframework.security.crypto.password.PasswordEncoder; // Ajouté
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder; // Ajouté pour le test de match
    private final ReinitialisationMotDePasseService reinitialisationService;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequestDTO loginRequest) {

        // --- DEBUT DU DEBUG ---
        System.out.println("=== DEBUG AUTHENTICATION ===");
        System.out.println("1. Tentative de login pour : " + loginRequest.getEmail());
        System.out.println("2. Mot de passe reçu du front : [" + loginRequest.getPassword() + "]");

        // On vérifie manuellement ce qu'il y a en base
        userRepository.findByEmail(loginRequest.getEmail()).ifPresentOrElse(user -> {
            System.out.println("3. Utilisateur trouvé en base !");
            System.out.println("4. Hash stocké en base : " + user.getMotDePasse());
            System.out.println("5. Compte actif en base ? : " + user.getActif());
            
            boolean isMatch = passwordEncoder.matches(loginRequest.getPassword(), user.getMotDePasse());
            System.out.println("6. Est-ce que le mot de passe match le hash ? " + (isMatch ? "OUI ✅" : "NON ❌"));
        }, () -> {
            System.out.println("3. ERREUR : Aucun utilisateur trouvé avec cet email en base !");
        });
        // --- FIN DU DEBUG ---

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

            System.out.println("7. AUTHENTIFICATION RÉUSSIE ! Génération du token...");
            return ResponseEntity.ok(new JwtResponseDTO(jwt, user.getId(), user.getEmail(), role));

        } catch (Exception e) {
            System.out.println("7. ÉCHEC AUTHENTIFICATION : " + e.getMessage());
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