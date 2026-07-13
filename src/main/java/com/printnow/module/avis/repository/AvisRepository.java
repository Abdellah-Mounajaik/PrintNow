package com.printnow.module.avis.repository;

import com.printnow.module.avis.model.Avis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AvisRepository extends JpaRepository<Avis, Long> {

    List<Avis> findByImprimerie_IdOrderByDateCreationDesc(Long imprimerieId);

    boolean existsByUser_IdAndImprimerie_Id(Long userId, Long imprimerieId);

    long countByImprimerie_Id(Long imprimerieId);

    @Query("SELECT AVG(a.note) FROM Avis a WHERE a.imprimerie.id = :imprimerieId")
    Double moyenneNoteByImprimerie(@Param("imprimerieId") Long imprimerieId);
}
