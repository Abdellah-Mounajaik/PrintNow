package com.printnow.module.studio.repository;

import com.printnow.module.studio.model.GenerationSupport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface GenerationSupportRepository extends JpaRepository<GenerationSupport, Long> {
    Optional<GenerationSupport> findBySuivi(String suivi);

    /**
     * Générations dont la dernière activité remonte à plus longtemps que la
     * limite — candidates à la purge RGPD. Les propositions sont chargées avec
     * (LEFT JOIN FETCH) pour effacer leurs fichiers sans requête supplémentaire.
     */
    @Query("SELECT DISTINCT g FROM GenerationSupport g LEFT JOIN FETCH g.propositions WHERE g.dateMaj < :limite")
    List<GenerationSupport> findAPurger(@Param("limite") LocalDateTime limite);
}
