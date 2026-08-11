package com.printnow.module.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.printnow.infrastructure.email.EmailService;
import com.printnow.module.user.dto.ChangePasswordRequestDTO;
import com.printnow.module.user.dto.SignupRequestDTO;
import com.printnow.module.user.dto.UpdateProfileRequestDTO;
import com.printnow.module.user.dto.UserRequestDTO;
import com.printnow.module.user.dto.UserResponseDTO;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import com.printnow.module.user.mapper.UserMapper;
import com.printnow.module.user.model.User;
import com.printnow.module.user.repository.RoleRepository;
import com.printnow.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.printnow.module.user.model.Role;
import com.printnow.module.user.dto.SignupRequestDTO;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor

public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public UserResponseDTO createUser(UserRequestDTO dto) {
        User user = userMapper.toEntity(dto);
        
        user.setMotDePasse(passwordEncoder.encode(dto.getMotDePasse()));
        
        if (dto.getRoleId() != null) {
            user.setRole(roleRepository.findById(dto.getRoleId()).orElse(null));
        }
        
        user.setActif(true);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public UserResponseDTO getMonProfil(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur non trouvé."));
        return userMapper.toResponse(user);
    }

    /**
     * Liste les comptes, y compris ceux qui ont été supprimés.
     *
     * Ces derniers ne contiennent plus rien de personnel, mais les masquer
     * laisserait l'administration sans explication : les commandes d'un compte
     * supprimé s'affichent au nom de « Compte supprimé », et il faut pouvoir
     * retrouver la ligne à laquelle elles renvoient.
     */
    @Transactional(readOnly = true)
    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toResponse)
                .collect(Collectors.toList());
    }

    /** Identifiant du compte connecté, pour les actions qui ne prennent jamais d'id du client. */
    @Transactional(readOnly = true)
    public Long idDeLUtilisateur(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur non trouvé."))
                .getId();
    }

    @Transactional(readOnly = true)
    public Optional<UserResponseDTO> getUserById(Long id) {
        // On cherche l'entité, et si elle existe, on la transforme en DTO
        return userRepository.findById(id)
                .map(userMapper::toResponse);
    }
    
    public UserResponseDTO updateUser(Long id, UserRequestDTO dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        userMapper.updateEntityFromDto(dto, user);
        
        return userMapper.toResponse(userRepository.save(user));
    }
    /**
     * @deprecated La suppression passe désormais par
     * {@link com.printnow.module.user.service.SuppressionCompteService}, qui
     * anonymise le compte au lieu de détruire la ligne — six tables y renvoient,
     * dont les commandes et les factures.
     */
    @Deprecated
    public void deleteUser(Long id) {
        throw new UnsupportedOperationException(
                "Utiliser SuppressionCompteService.supprimer : une ligne users détruite emporterait ses commandes.");
    }

    /**
     * Modifie le profil (nom, prénom, téléphone, email) de l'utilisateur
     * actuellement connecté — jamais le mot de passe (voir changePassword).
     */
    @Transactional
    public UserResponseDTO updateProfile(String currentEmail, UpdateProfileRequestDTO dto) {
        User user = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur non trouvé."));

        boolean emailChange = !dto.getEmail().equalsIgnoreCase(user.getEmail());
        if (emailChange && userRepository.existsByEmail(dto.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cet email est déjà utilisé par un autre compte.");
        }

        user.setPrenom(dto.getPrenom());
        user.setNom(dto.getNom());
        user.setTelephone(dto.getTelephone());
        user.setEmail(dto.getEmail());

        return userMapper.toResponse(userRepository.save(user));
    }

    /**
     * Change le mot de passe de l'utilisateur connecté, après vérification de
     * l'ancien mot de passe.
     */
    @Transactional
    public void changePassword(String currentEmail, ChangePasswordRequestDTO dto) {
        User user = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur non trouvé."));

        if (!passwordEncoder.matches(dto.getAncienMotDePasse(), user.getMotDePasse())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'ancien mot de passe est incorrect.");
        }

        user.setMotDePasse(passwordEncoder.encode(dto.getNouveauMotDePasse()));
        userRepository.save(user);
    }
    public UserResponseDTO registerClient(SignupRequestDTO dto) {
        // 1. Vérification que l'email n'est pas déjà pris
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Erreur : Cet email est déjà utilisé !");
        }

        // 2. Création de l'utilisateur (on fait le mapping manuellement ici pour plus de sécurité)
        User user = new User();
        user.setPrenom(dto.getPrenom());
        user.setNom(dto.getNom());
        user.setEmail(dto.getEmail());
        user.setTelephone(dto.getTelephone());
        
        // 3. Hashage du mot de passe !
        user.setMotDePasse(passwordEncoder.encode(dto.getMotDePasse()));
        user.setActif(true);

        // 4. Attribution du rôle par défaut (CLIENT)
        Role clientRole = roleRepository.findByNom("CLIENT")
                .orElseThrow(() -> new RuntimeException("Erreur : Le rôle CLIENT n'existe pas en base de données."));
        user.setRole(clientRole);

        // 5. Sauvegarde et retour via le Mapper
        UserResponseDTO response = userMapper.toResponse(userRepository.save(user));

        // 6. Mail de bienvenue (asynchrone, ne doit jamais faire échouer l'inscription)
        emailService.envoyerBienvenue(user.getEmail(), user.getPrenom());

        return response;
    }

}