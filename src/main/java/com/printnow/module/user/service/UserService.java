package com.printnow.module.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.printnow.module.user.dto.SignupRequestDTO;
import com.printnow.module.user.dto.UserRequestDTO;
import com.printnow.module.user.dto.UserResponseDTO;
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
    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toResponse)
                .collect(Collectors.toList());
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
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("Impossible de supprimer : Utilisateur non trouvé avec l'id : " + id);
        }
        userRepository.deleteById(id);
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
        return userMapper.toResponse(userRepository.save(user));
    }


}