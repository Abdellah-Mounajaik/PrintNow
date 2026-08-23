package com.printnow.module.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.printnow.module.order.enums.StatutCommande;
import com.printnow.module.order.model.Commande;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommandeRepository extends JpaRepository<Commande, Long> {
    
    // Trouver une commande par son numéro unique (ex: CMD-2025-012)
    Optional<Commande> findByNumeroCommande(String numeroCommande);

    // Ce paiement a-t-il donné lieu à une commande ? (voir PaiementAbandonneService)
    boolean existsByPaymentIntentId(String paymentIntentId);
    
    // Récupérer toutes les commandes d'un client précis
    List<Commande> findByClient_IdOrderByDateCreationDesc(Long clientId);

    // Commandes qui engagent encore l'imprimeur (voir SuppressionCompteService)
    long countByClient_IdAndStatutIn(Long clientId, java.util.Collection<StatutCommande> statuts);
    
    // Récupérer toutes les commandes d'une imprimerie (pour ton Dashboard Imprimeur !)
    List<Commande> findByImprimerie_IdOrderByDateCreationDesc(Long imprimerieId);
    
    // Récupérer les commandes d'une imprimerie filtrées par statut (ex: "EN_ATTENTE_PAIEMENT")
    List<Commande> findByImprimerie_IdAndStatut(Long imprimerieId, StatutCommande statut);

    long countByClient_IdAndCodePromo_Code(Long clientId, String code);

    List<Commande> findAllByOrderByDateCreationDesc();

    // Vérifie qu'un client a au moins une commande livrée chez une imprimerie (pour autoriser un avis)
    boolean existsByClient_IdAndImprimerie_IdAndStatut(Long clientId, Long imprimerieId, StatutCommande statut);

    /*
     * Éligibilité à un avis : LIVREE couvre la livraison, mais un retrait en
     * magasin ne dépasse jamais PRETE (l'imprimeur n'a pas de bouton pour
     * repointer chaque retrait au comptoir — voir DashboardImprimeur). Sans ce
     * cas particulier, aucun client en retrait magasin ne pourrait jamais
     * laisser d'avis.
     */
    @Query("SELECT COUNT(c) > 0 FROM Commande c WHERE c.client.id = :clientId AND c.imprimerie.id = :imprimerieId " +
            "AND (c.statut = com.printnow.module.order.enums.StatutCommande.LIVREE " +
            "OR (c.statut = com.printnow.module.order.enums.StatutCommande.PRETE " +
            "AND c.modeRetrait = com.printnow.module.order.enums.ModeRetrait.RETRAIT_MAGASIN))")
    boolean existsCommandeEligibleAvis(@Param("clientId") Long clientId, @Param("imprimerieId") Long imprimerieId);

    /*
     * Requêtes des tableaux de bord (client, imprimeur, admin).
     *
     * Sans elles, afficher une liste de commandes déclenchait une cinquantaine
     * d'allers-retours vers la base — un par commande pour son client, son
     * imprimerie, sa livraison, ses lignes, etc. Chaque aller-retour coûte cher
     * (base dans Docker), d'où plusieurs secondes d'attente.
     *
     * Ici tout est ramené en une seule requête : commande + lignes + produit +
     * client + imprimerie + livraison + adresse. Seuls les fichiers (2ᵉ
     * collection, qui provoquerait un produit cartésien avec les lignes) sont
     * laissés au regroupement par lots (hibernate.default_batch_fetch_size).
     */
    String FETCH_TABLEAU_DE_BORD =
            "SELECT DISTINCT c FROM Commande c " +
            "LEFT JOIN FETCH c.lignes l " +
            "LEFT JOIN FETCH l.produit " +
            "LEFT JOIN FETCH c.client " +
            "LEFT JOIN FETCH c.imprimerie im " +
            "LEFT JOIN FETCH c.livraison " +
            "LEFT JOIN FETCH c.adresseLivraison ";

    @Query(FETCH_TABLEAU_DE_BORD + "ORDER BY c.dateCreation DESC")
    List<Commande> findAllPourTableauDeBord();

    @Query(FETCH_TABLEAU_DE_BORD + "WHERE im.id = :imprimerieId ORDER BY c.dateCreation DESC")
    List<Commande> findParImprimeriePourTableauDeBord(@Param("imprimerieId") Long imprimerieId);

    @Query(FETCH_TABLEAU_DE_BORD + "WHERE c.client.id = :clientId ORDER BY c.dateCreation DESC")
    List<Commande> findParClientPourTableauDeBord(@Param("clientId") Long clientId);
}