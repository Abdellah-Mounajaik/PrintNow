package com.printnow.module.correction.service;

import com.printnow.module.correction.model.VerificationOrthographe;
import com.printnow.module.correction.repository.VerificationOrthographeRepository;
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
 * Efface les documents de la correction orthographique une fois le service rendu.
 *
 * Un client dépose son fichier pour en corriger les fautes ; PrintNow en garde
 * l'original et en produit une version corrigée. Ces documents ne servent qu'à
 * la correction elle-même — aucune impression ni facture n'en dépend — et
 * n'ont donc plus de raison d'être conservés une fois la correction réglée ou
 * abandonnée. Le RGPD impose alors de les effacer (article 5.1.e), tout comme
 * le résultat de l'analyse, qui contient des extraits du texte du client.
 *
 * On laisse passer un court délai après la dernière activité, le temps qu'une
 * correction en cours d'achat soit menée à son terme (l'application d'une
 * correction relit l'original) et qu'un client ayant payé puisse retélécharger.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PurgeCorrectionsService {

    private final VerificationOrthographeRepository repository;

    /** Délai de grâce après la dernière activité, en jours (configurable). */
    @Value("${purge.corrections.retention-jours:7}")
    private long retentionJours;

    /**
     * Une fois par jour, à 3 h 15. Une exécution manquée est sans conséquence :
     * les corrections concernées seront simplement effacées le lendemain.
     */
    @Scheduled(cron = "${purge.corrections.cron:0 15 3 * * *}")
    @Transactional
    public void purgerLesCorrections() {
        LocalDateTime limite = LocalDateTime.now().minusDays(retentionJours);
        List<VerificationOrthographe> aEffacer = repository.findAPurger(limite);
        if (aEffacer.isEmpty()) return;

        int fichiersEffaces = 0;
        for (VerificationOrthographe v : aEffacer) {
            if (effacerDuDisque(v.getCheminOriginal())) fichiersEffaces++;
            if (effacerDuDisque(v.getCheminCorrige())) fichiersEffaces++;
        }
        // On retire aussi l'enregistrement : le résultat de l'analyse conserve des
        // extraits du document, il n'a pas à survivre aux fichiers.
        repository.deleteAllInBatch(aEffacer);

        log.info("Purge RGPD : {} correction(s) effacée(s) ({} fichiers retirés du disque) — "
                        + "dernière activité il y a plus de {} jour(s)",
                aEffacer.size(), fichiersEffaces, retentionJours);
    }

    private boolean effacerDuDisque(String chemin) {
        if (chemin == null || chemin.isBlank()) return false;
        try {
            return Files.deleteIfExists(Paths.get(chemin));
        } catch (IOException e) {
            log.warn("Document de correction impossible à effacer ({}) : {}", chemin, e.getMessage());
            return false;
        }
    }
}
