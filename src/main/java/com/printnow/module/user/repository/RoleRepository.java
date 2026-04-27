package com.printnow.module.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.printnow.module.user.model.Role;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    // Utile pour l'initialisation ou l'attribution de rôles
    Optional<Role> findByNom(String nom);
}