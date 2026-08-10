package com.printnow.module.user.repository;

import com.printnow.module.user.model.JetonReinitialisation;
import com.printnow.module.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface JetonReinitialisationRepository extends JpaRepository<JetonReinitialisation, Long> {

    Optional<JetonReinitialisation> findByEmpreinte(String empreinte);

    List<JetonReinitialisation> findByUtilisateurAndUtiliseLeIsNull(User utilisateur);

    /** Purge des jetons périmés, pour que la table ne grossisse pas indéfiniment. */
    void deleteByExpireLeBefore(LocalDateTime instant);
}
