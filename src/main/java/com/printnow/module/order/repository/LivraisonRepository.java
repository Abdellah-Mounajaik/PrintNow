package com.printnow.module.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.printnow.module.order.enums.StatutLivraison;
import com.printnow.module.order.model.Livraison;

import java.util.List;
import java.util.Optional;

@Repository
public interface LivraisonRepository extends JpaRepository<Livraison, Long> {

    /**
     * Retrouver une livraison par son numéro de suivi unique (ex: DHL123...)
     */
    Optional<Livraison> findByNumeroSuivi(String numeroSuivi);

    /**
     * Lister toutes les livraisons ayant un certain statut
     * Utile pour le dashboard imprimeur (ex: voir tout ce qui est EN_COURS)
     */
    List<Livraison> findByStatutLivraison(StatutLivraison statut);

    /**
     * Retrouver la livraison associée à une commande spécifique
     */
    Optional<Livraison> findByCommandeId(Long commandeId);
    
    /**
     * Vérifier si un numéro de suivi existe déjà avant de l'attribuer
     */
    boolean existsByNumeroSuivi(String numeroSuivi);
}