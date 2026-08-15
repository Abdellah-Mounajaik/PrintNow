package com.printnow.module.studio.repository;

import com.printnow.module.studio.model.GenerationSupport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GenerationSupportRepository extends JpaRepository<GenerationSupport, Long> {
    Optional<GenerationSupport> findBySuivi(String suivi);
}
