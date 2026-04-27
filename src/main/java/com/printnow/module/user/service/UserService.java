package com.printnow.module.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.printnow.module.user.dto.UserRequestDTO;
import com.printnow.module.user.dto.UserResponseDTO;
import com.printnow.module.user.mapper.UserMapper;
import com.printnow.module.user.model.User;
import com.printnow.module.user.repository.RoleRepository;
import com.printnow.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;


import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;

    public UserResponseDTO createUser(UserRequestDTO dto) {
        User user = userMapper.toEntity(dto);
        
        // Gestion du rôle
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

}