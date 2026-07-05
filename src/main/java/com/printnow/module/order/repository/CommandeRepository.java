package com.printnow.module.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.printnow.module.order.enums.StatutCommande;
import com.printnow.module.order.model.Commande;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommandeRepository extends JpaRepository<Commande, Long> {
    
    // Trouver une commande par son numéro unique (ex: CMD-2025-012)
    Optional<Commande> findByNumeroCommande(String numeroCommande);
    
    // Récupérer toutes les commandes d'un client précis
    List<Commande> findByClient_IdOrderByDateCreationDesc(Long clientId);
    
    // Récupérer toutes les commandes d'une imprimerie (pour ton Dashboard Imprimeur !)
    List<Commande> findByImprimerie_IdOrderByDateCreationDesc(Long imprimerieId);
    
    // Récupérer les commandes d'une imprimerie filtrées par statut (ex: "EN_ATTENTE_PAIEMENT")
    List<Commande> findByImprimerie_IdAndStatut(Long imprimerieId, StatutCommande statut);

    long countByClient_IdAndCodePromo_Code(Long clientId, String code);

    List<Commande> findAllByOrderByDateCreationDesc();
}