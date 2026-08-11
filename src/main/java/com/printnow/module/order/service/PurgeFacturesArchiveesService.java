package com.printnow.module.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Efface les factures archivées dont la conservation n'est plus obligatoire.
 *
 * Ces copies ne subsistent que parce que la loi impose de garder ses factures
 * sept ans ; c'est ce seul motif qui autorise à conserver le nom d'un client
 * ayant demandé son effacement. Le délai écoulé, le motif disparaît et le RGPD
 * interdit de garder ces données plus longtemps (article 5.1.e).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PurgeFacturesArchiveesService {

    private final DepotFacturesArchivees depot;

    /**
     * Une fois par jour. Une purge manquée n'a aucune conséquence : la facture
     * suivante sera simplement effacée le lendemain.
     */
    @Scheduled(cron = "0 30 3 * * *")
    public void purgerLesFacturesPerimees() {
        int effacees = depot.effacerLesArchivesPerimees(LocalDate.now());
        if (effacees > 0) {
            log.info("{} facture(s) archivée(s) effacée(s) : leur durée de conservation légale est écoulée", effacees);
        }
    }
}
