package com.printnow.module.order.repository;

import com.printnow.module.order.model.FichierPDF;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FichierPDFRepository extends JpaRepository<FichierPDF, Long> {
    List<FichierPDF> findByLigneCommande_Id(Long ligneCommandeId);
}
