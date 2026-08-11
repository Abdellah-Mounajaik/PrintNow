package com.printnow.module.order.service;

import com.printnow.module.order.model.Commande;
import com.printnow.module.order.repository.CommandeRepository;
import com.printnow.module.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Fige sur disque les factures d'un client avant que son compte ne soit effacé.
 *
 * Les factures sont d'ordinaire fabriquées à la demande depuis la base : une
 * fois le nom du client anonymisé, il n'en resterait plus aucune version
 * nominative. Or la loi impose de conserver ses factures sept ans, et une
 * facture doit désigner son destinataire — l'imprimerie, qui est le vendeur,
 * comme PrintNow qui a émis le document. On en archive donc une copie au moment
 * de la suppression : c'est elle qui portera désormais l'identité du client, ce
 * qui autorise à l'effacer partout ailleurs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ArchiveFactureService {

    private final CommandeRepository commandeRepository;
    private final FactureService factureService;
    private final DepotFacturesArchivees depot;

    /**
     * Archive les factures des commandes du client.
     *
     * @return le nombre de factures écrites
     */
    @Transactional(readOnly = true)
    public int archiverLesFacturesDe(User client) {
        List<Commande> commandes = commandeRepository.findByClient_IdOrderByDateCreationDesc(client.getId());
        int archivees = 0;

        for (Commande commande : commandes) {
            if (!factureService.estFacturable(commande)) continue; // rien à facturer
            if (archiver(commande)) archivees++;
        }

        if (archivees > 0) {
            log.info("{} facture(s) archivée(s) avant la suppression du compte {}", archivees, client.getId());
        }
        return archivees;
    }

    private boolean archiver(Commande commande) {
        try {
            if (depot.existe(commande.getNumeroCommande())) return false; // déjà figée

            depot.ecrire(commande.getNumeroCommande(), factureService.genererPourArchivage(commande));
            return true;

        } catch (Exception e) {
            // L'échec est signalé mais n'interrompt pas la suppression : refuser
            // d'effacer un compte parce qu'un PDF n'a pas pu s'écrire priverait
            // la personne d'un droit pour une raison technique.
            log.error("Facture de la commande {} non archivée — à reconstituer manuellement avant "
                    + "que les données du client ne soient effacées", commande.getNumeroCommande(), e);
            return false;
        }
    }
}
