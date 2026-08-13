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
     * Fichiers clients dont la commande est terminée depuis assez longtemps pour
     * qu'on n'en ait plus besoin (voir PurgeFichiersClientsService).
     *
     * La date de référence est celle de la dernière mise à jour de la commande —
     * en pratique son passage à « livrée ». Pour les commandes antérieures à
     * l'ajout de ce champ, elle vaut null : on retombe alors sur la date de
     * création, largement dépassée.
     */
    @Query("SELECT f FROM FichierPDF f " +
           "WHERE f.ligneCommande.commande.statut IN :statutsTermines " +
           "AND COALESCE(f.ligneCommande.commande.dateMiseAJour, f.ligneCommande.commande.dateCreation) < :limite")
    List<FichierPDF> findClientsAPurger(@Param("statutsTermines") Collection<StatutCommande> statutsTermines,
                                        @Param("limite") LocalDateTime limite);
}
