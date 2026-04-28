package com.printnow.module.user.controller;

import com.printnow.module.user.dto.JwtResponseDTO;
import com.printnow.module.user.dto.LoginRequestDTO;
import com.printnow.module.user.dto.SignupRequestDTO;
import com.printnow.module.user.dto.UserResponseDTO;
import com.printnow.module.user.model.User;
import com.printnow.module.user.repository.UserRepository;
import com.printnow.module.user.service.UserService;
import com.printnow.infrastructure.security.JwtUtils;
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

    @PostMapping("/login")
    public ResponseEntity<JwtResponseDTO> authenticateUser(@RequestBody LoginRequestDTO loginRequest) {

        // 1. Spring Security vérifie l'email et le mot de passe
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getMotDePasse()));

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
        
    }
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody SignupRequestDTO signUpRequest) {
        try {
            UserResponseDTO newUser = userService.registerClient(signUpRequest);
            return new ResponseEntity<>(newUser, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            // Si l'email existe déjà, on renvoie une erreur 400 Bad Request
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
}