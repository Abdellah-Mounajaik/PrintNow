package com.printnow.module.shop.repository;

import com.printnow.module.shop.model.Produit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProduitRepository extends JpaRepository<Produit, Long> {
    
    // Récupérer tout le catalogue actif d'une imprimerie
    List<Produit> findByImprimerieIdAndActifTrue(Long imprimerieId);
}