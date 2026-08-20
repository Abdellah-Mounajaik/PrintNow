package com.printnow.module.shop.repository;

import org.springframework.stereotype.Repository;
import com.printnow.module.shop.model.Imprimerie;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;



@Repository
public interface ImprimerieRepository extends JpaRepository<Imprimerie, Long> {
    
    // Récupérer toutes les imprimeries visibles par les clients
    List<Imprimerie> findAllByActifTrue();
    
    // Récupérer les imprimeries gérées par un utilisateur spécifique
    List<Imprimerie> findByGerantId(Long idGerant);

    // Retrouver une imprimerie depuis l'adresse lisible de sa fiche
    Optional<Imprimerie> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsByNumeroTva(String numeroTva);

    boolean existsByPaymentIntentId(String paymentIntentId);

    // Imprimeries dont l'adresse lisible reste à calculer (créées avant sa mise en place)
    List<Imprimerie> findBySlugIsNull();

}