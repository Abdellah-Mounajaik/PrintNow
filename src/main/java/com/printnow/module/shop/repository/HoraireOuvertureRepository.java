package com.printnow.module.shop.repository;

import com.printnow.module.shop.model.HoraireOuverture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HoraireOuvertureRepository extends JpaRepository<HoraireOuverture, Long> {
    
    // Trouver tous les horaires liés à une imprimerie
    List<HoraireOuverture> findByImprimerieId(Long imprimerieId);
}