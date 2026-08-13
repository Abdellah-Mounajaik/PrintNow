package com.printnow.module.order.service;

import com.printnow.module.order.enums.StatutCommande;
import com.printnow.module.order.model.FichierPDF;
import com.printnow.module.order.repository.FichierPDFRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Efface les documents téléversés par les clients une fois la commande terminée.
 *
 * Un PDF n'est collecté que pour être imprimé. La commande livrée ou annulée, il
 * n'a plus de raison d'être conservé — le RGPD impose alors de l'effacer
 * (article 5.1.e, limitation de la conservation). On laisse néanmoins passer un
 * court délai après la clôture, le temps qu'une réimpression ou une réclamation
 * reste possible ; au-delà, le fichier disparaît du disque et de la base.
 *
 * La facture, elle, n'est pas concernée : ce n'est pas le document imprimé mais
 * une pièce comptable, conservée sept ans à part (voir
 * {@link PurgeFacturesArchiveesService}).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PurgeFichiersClientsService {

    /** Une commande n'est « terminée » que dans ces états. */
    private static final List<StatutCommande> STATUTS_TERMINES =
            List.of(StatutCommande.LIVREE, StatutCommande.ANNULEE);

    private final FichierPDFRepository fichierPDFRepository;

    /** Délai de grâce après la clôture, en jours (configurable). */
    @Value("${purge.fichiers-clients.retention-jours:7}")
    private long retentionJours;

    /**
     * Une fois par jour, à 3 h 45 (après la purge des factures). Une exécution
     * manquée est sans conséquence : les fichiers concernés seront simplement
     * effacés le lendemain.
     */
    @Scheduled(cron = "${purge.fichiers-clients.cron:0 45 3 * * *}")
    @Transactional
    public void purgerLesFichiersClients() {
        LocalDateTime limite = LocalDateTime.now().minusDays(retentionJours);
        List<FichierPDF> aEffacer = fichierPDFRepository.findClientsAPurger(STATUTS_TERMINES, limite);
        if (aEffacer.isEmpty()) return;

        int effacesDuDisque = 0;
        for (FichierPDF fichier : aEffacer) {
            if (effacerDuDisque(fichier.getCheminStockage())) effacesDuDisque++;
        }
        // On retire les métadonnées ensuite : le nom du fichier lui-même peut
        // révéler des informations personnelles, il n'a pas à survivre au contenu.
        fichierPDFRepository.deleteAllInBatch(aEffacer);

        log.info("Purge RGPD : {} fichier(s) client effacé(s) ({} retirés du disque) — "
                        + "commande terminée depuis plus de {} jour(s)",
                aEffacer.size(), effacesDuDisque, retentionJours);
    }

    private boolean effacerDuDisque(String chemin) {
        if (chemin == null || chemin.isBlank()) return false;
        try {
            return Files.deleteIfExists(Paths.get(chemin));
        } catch (IOException e) {
            // Un fichier introuvable ou verrouillé ne doit pas interrompre la purge ;
            // la ligne en base est tout de même retirée, et l'incident est tracé.
            log.warn("Fichier client impossible à effacer ({}) : {}", chemin, e.getMessage());
            return false;
        }
    }
}
