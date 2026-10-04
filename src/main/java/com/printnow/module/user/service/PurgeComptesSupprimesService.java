package com.printnow.module.user.service;

import com.printnow.module.user.model.User;
import com.printnow.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Efface les données des comptes fermés dont le délai de rétractation est
 * écoulé.
 *
 * La suppression d'un compte se fait en deux temps : fermeture immédiate, puis
 * effacement. Ce service se charge du second, et c'est lui qui rend la
 * suppression effective au sens du RGPD — tant qu'il n'est pas passé, les
 * données personnelles sont toujours là.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PurgeComptesSupprimesService {

    private final UserRepository userRepository;
    private final SuppressionCompteService suppressionCompteService;

    /**
     * Une fois par jour. Un passage manqué ne fait que décaler l'effacement au
     * lendemain, bien avant que le retard ne devienne discutable.
     */
    @Scheduled(cron = "0 0 4 * * *")
    public void anonymiserLesComptesEchus() {
        LocalDateTime limite = LocalDateTime.now()
                .minusDays(SuppressionCompteService.JOURS_AVANT_ANONYMISATION);

        List<User> echus = userRepository
                .findByDateSuppressionBeforeAndDateAnonymisationIsNull(limite);
        if (echus.isEmpty()) return;

        // Un compte qui échoue ne doit pas empêcher les suivants d'être traités :
        // les données des autres n'ont pas à attendre un jour de plus.
        int anonymises = 0;
        for (User utilisateur : echus) {
            try {
                suppressionCompteService.anonymiserDefinitivement(utilisateur);
                anonymises++;
            } catch (Exception e) {
                log.error("Anonymisation impossible pour le compte {}", utilisateur.getId(), e);
            }
        }

        log.info("{} compte(s) anonymise(s) : leur delai de retractation est ecoule", anonymises);
    }
}
