package com.printnow.module.user.repository;

import com.printnow.module.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Indispensable pour la sécurité et la connexion
    Optional<User> findByEmail(String email);

    // Pour vérifier si un email existe déjà lors de l'inscription
    Boolean existsByEmail(String email);

    // Rechercher tous les utilisateurs actifs (basé sur le champ 'actif' du modèle)
    List<User> findAllByActifTrue();

    // Les comptes supprimés restent en base pour que commandes et factures
    // gardent un sens, mais n'ont plus à figurer dans les listes.
    List<User> findByDateSuppressionIsNull();

    // Rechercher les utilisateurs par rôle (ex: trouver tous les ADMINS)
    List<User> findByRoleNom(String roleNom);
}
