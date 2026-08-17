package com.printnow.module.studio.service;

import com.printnow.module.studio.model.GenerationSupport;
import com.printnow.module.studio.model.PropositionSupport;
import com.printnow.module.studio.repository.GenerationSupportRepository;
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
 * Efface les designs générés par l'IA (studio) une fois qu'ils ont fait leur office.
 *
 * Une génération produit des PDF et leurs aperçus, à partir d'un brief et d'un
 * contenu structuré qui relèvent de la donnée personnelle (un CV porte nom,
 * téléphone, parcours…). Ces fichiers ne servent qu'à la phase de choix : quand
 * le client retient une proposition, la commande en garde sa <b>propre</b> copie
 * — rien d'imprimable ni de comptable ne dépend donc du studio. Le RGPD impose
 * de ne pas les conserver au-delà de leur utilité (article 5.1.e).
 *
 * On laisse passer un court délai après la dernière activité, le temps qu'une
 * commande en cours d'achat soit menée à son terme ; au-delà, générations,
 * propositions et fichiers disparaissent.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PurgeGenerationsService {

    private final GenerationSupportRepository repository;

    /** Délai de grâce après la dernière activité, en jours (configurable). */
    @Value("${purge.generations.retention-jours:7}")
    private long retentionJours;

    /**
     * Une fois par jour, à 3 h 30 (entre la purge des corrections et celle des
     * fichiers clients). Une exécution manquée est sans conséquence : les
     * générations concernées seront simplement effacées le lendemain.
     */
    @Scheduled(cron = "${purge.generations.cron:0 30 3 * * *}")
    @Transactional
    public void purgerLesGenerations() {
        LocalDateTime limite = LocalDateTime.now().minusDays(retentionJours);
        List<GenerationSupport> aEffacer = repository.findAPurger(limite);
        if (aEffacer.isEmpty()) return;

        int fichiersEffaces = 0;
        for (GenerationSupport g : aEffacer) {
            for (PropositionSupport p : g.getPropositions()) {
                if (effacerDuDisque(p.getCheminPdf())) fichiersEffaces++;
                if (effacerDuDisque(p.getCheminApercu())) fichiersEffaces++;
            }
        }
        // deleteAll (et non deleteAllInBatch) pour laisser la cascade retirer les
        // propositions ; le brief et le contenu conservent des données du client,
        // ils n'ont pas à survivre aux fichiers.
        repository.deleteAll(aEffacer);

        log.info("Purge RGPD : {} génération(s) IA effacée(s) ({} fichiers retirés du disque) — "
                        + "dernière activité il y a plus de {} jour(s)",
                aEffacer.size(), fichiersEffaces, retentionJours);
    }

    private boolean effacerDuDisque(String chemin) {
        if (chemin == null || chemin.isBlank()) return false;
        try {
            return Files.deleteIfExists(Paths.get(chemin));
        } catch (IOException e) {
            log.warn("Design IA impossible à effacer ({}) : {}", chemin, e.getMessage());
            return false;
        }
    }
}
