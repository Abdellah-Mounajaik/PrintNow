package com.printnow.module.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.printnow.module.user.dto.ChangePasswordRequestDTO;
import com.printnow.module.user.dto.UpdateProfileRequestDTO;
import com.printnow.module.user.dto.UserRequestDTO;
import com.printnow.module.user.dto.UserResponseDTO;
import com.printnow.module.user.service.SuppressionCompteService;
import com.printnow.module.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;


import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final SuppressionCompteService suppressionCompteService;

    // Les endpoints ci-dessous manipulent un utilisateur par id arbitraire —
    // réservés à l'administration. La modification de son propre profil passe
    // uniquement par /me et /me/password, plus bas, qui ignorent tout id fourni
    // par le client et se basent sur le token.
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDTO> createUser(@RequestBody UserRequestDTO userRequestDTO) {
        return new ResponseEntity<>(userService.createUser(userRequestDTO), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDTO> updateUser(@PathVariable Long id, @RequestBody UserRequestDTO userRequestDTO) {
        return ResponseEntity.ok(userService.updateUser(id, userRequestDTO));
    }

    /**
     * DELETE /api/users/{id}
     * Supprime le compte d'un utilisateur (administration).
     *
     * La ligne subsiste — commandes et factures y renvoient — mais ses données
     * personnelles sont effacées et il ne peut plus se connecter.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        String admin = SecurityContextHolder.getContext().getAuthentication().getName();
        suppressionCompteService.supprimer(id, "l'administrateur " + admin);
        return ResponseEntity.noContent().build();
    }

    /**
     * DELETE /api/users/me
     * Permet à un utilisateur de supprimer son propre compte, comme le prévoit
     * le droit à l'effacement. L'id n'est jamais pris dans la requête.
     */
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMyAccount() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        suppressionCompteService.supprimer(userService.idDeLUtilisateur(email), "l'intéressé");
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/users/me
     * Renvoie le profil complet de l'utilisateur connecté.
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getMyProfile() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(userService.getMonProfil(email));
    }

    /**
     * PUT /api/users/me
     * Modifie le profil de l'utilisateur connecté (jamais un autre id — on ne
     * fait jamais confiance à un id passé par le client pour ça).
     */
    @PutMapping("/me")
    public ResponseEntity<UserResponseDTO> updateMyProfile(@Valid @RequestBody UpdateProfileRequestDTO dto) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(userService.updateProfile(email, dto));
    }

    /**
     * PUT /api/users/me/password
     * Change le mot de passe de l'utilisateur connecté.
     */
    @PutMapping("/me/password")
    public ResponseEntity<Void> changeMyPassword(@Valid @RequestBody ChangePasswordRequestDTO dto) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        userService.changePassword(email, dto);
        return ResponseEntity.noContent().build();
    }
}