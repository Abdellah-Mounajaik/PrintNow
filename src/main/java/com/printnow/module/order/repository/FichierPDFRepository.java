package com.printnow.module.order.repository;

import com.printnow.module.order.enums.StatutCommande;
import com.printnow.module.order.model.FichierPDF;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface FichierPDFRepository extends JpaRepository<FichierPDF, Long> {
    List<FichierPDF> findByLigneCommande_Id(Long ligneCommandeId);

    /**
     * Fichiers clients dont la commande n'attend plus rien d'eux (voir
     * PurgeFichiersClientsService).
     *
     * Trois cas, et non le seul statut « terminé » :
     *  - livrée ou annulée : le parcours est allé à son terme ;
     *  - prête en retrait magasin : le document est déjà imprimé et attend au
     *    comptoir, et ce statut est le dernier qu'une commande en retrait
     *    atteigne — s'en tenir à « livrée » conservait ces fichiers à jamais ;
     *  - jamais payée : la commande n'a pas abouti, personne ne les réclamera.
     *
     * La date de référence est celle de la dernière mise à jour de la commande.
     * Pour les commandes antérieures à l'ajout de ce champ, elle vaut null : on
     * retombe alors sur la date de création, largement dépassée.
     */
    @Query("SELECT f FROM FichierPDF f " +
           "WHERE COALESCE(f.ligneCommande.commande.dateMiseAJour, f.ligneCommande.commande.dateCreation) < :limite " +
           "AND (f.ligneCommande.commande.statut IN :statutsTermines " +
           "  OR f.ligneCommande.commande.statut = com.printnow.module.order.enums.StatutCommande.EN_ATTENTE_PAIEMENT " +
           "  OR (f.ligneCommande.commande.statut = com.printnow.module.order.enums.StatutCommande.PRETE " +
           "      AND f.ligneCommande.commande.modeRetrait = com.printnow.module.order.enums.ModeRetrait.RETRAIT_MAGASIN))")
    List<FichierPDF> findClientsAPurger(@Param("statutsTermines") Collection<StatutCommande> statutsTermines,
                                        @Param("limite") LocalDateTime limite);
}
