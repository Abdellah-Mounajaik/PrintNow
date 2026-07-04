package com.printnow.module.promo.repository;

import com.printnow.module.promo.model.CodePromo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CodePromoRepository extends JpaRepository<CodePromo, Long> {
    Optional<CodePromo> findByCodeIgnoreCaseAndActifTrueAndSupprimeFalse(String code);
    Optional<CodePromo> findByCodeIgnoreCaseAndSupprimeTrue(String code);
    java.util.List<CodePromo> findByImprimerie_IdAndSupprimeFalseOrderByIdDesc(Long imprimerieId);
}
