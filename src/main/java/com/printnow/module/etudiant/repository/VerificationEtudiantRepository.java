package com.printnow.module.etudiant.repository;

import com.printnow.module.etudiant.model.VerificationEtudiant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VerificationEtudiantRepository extends JpaRepository<VerificationEtudiant, Long> {
    Optional<VerificationEtudiant> findByUser_Id(Long userId);
    List<VerificationEtudiant> findAllByOrderByDateSoumissionDesc();
}
