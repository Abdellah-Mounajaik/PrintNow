package com.printnow.module.correction.repository;

import com.printnow.module.correction.model.VerificationOrthographe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface VerificationOrthographeRepository extends JpaRepository<VerificationOrthographe, Long> {

    /**
     * Corrections dont la dernière activité remonte à assez longtemps pour être
     * purgées (voir PurgeCorrectionsService).
     *
     * La référence est la date de paiement quand elle existe — la correction a
     * alors été réglée avec une commande — sinon la date d'analyse, pour couvrir
     * les corrections simplement essayées puis abandonnées.
     */
    @Query("SELECT v FROM VerificationOrthographe v " +
           "WHERE COALESCE(v.datePaiement, v.dateCreation) < :limite")
    List<VerificationOrthographe> findAPurger(@Param("limite") LocalDateTime limite);
}
