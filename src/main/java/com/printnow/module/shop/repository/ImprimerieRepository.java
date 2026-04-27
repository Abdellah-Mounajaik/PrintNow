package com.printnow.module.shop.repository;

import org.springframework.stereotype.Repository;
import com.printnow.module.shop.model.Imprimerie;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;



@Repository
public interface ImprimerieRepository extends JpaRepository<Imprimerie, Long> {
    
    // Récupérer toutes les imprimeries visibles par les clients
    List<Imprimerie> findAllByActifTrue();
    
    // Récupérer les imprimeries gérées par un utilisateur spécifique
    List<Imprimerie> findByIdGerant(Long idGerant);
}