package com.printnow.module.order.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La purge n'agira qu'en 2034 : ces cas la mettent à l'épreuve dès maintenant,
 * en lui présentant des dates choisies.
 */
class DepotFacturesArchiveesTest {

    @TempDir
    Path dossierTemporaire;

    private DepotFacturesArchivees depotSur(Path racine) {
        DepotFacturesArchivees depot = new DepotFacturesArchivees();
        ReflectionTestUtils.setField(depot, "archiveDir", racine.toString());
        return depot;
    }

    private void deposer(String numeroCommande, DepotFacturesArchivees depot) throws IOException {
        depot.ecrire(numeroCommande, "facture".getBytes());
    }

    @Test
    void conserve_une_facture_pendant_les_sept_annees_qui_suivent() throws IOException {
        DepotFacturesArchivees depot = depotSur(dossierTemporaire);
        deposer("CMD-20260811-A1B2C", depot);

        // Une facture de 2026 se conserve jusqu'au 31 décembre 2033.
        assertThat(depot.effacerLesArchivesPerimees(LocalDate.of(2033, 12, 31))).isZero();
        assertThat(depot.lire("CMD-20260811-A1B2C")).isPresent();
    }

    @Test
    void efface_la_facture_le_lendemain_de_l_echeance() throws IOException {
        DepotFacturesArchivees depot = depotSur(dossierTemporaire);
        deposer("CMD-20260811-A1B2C", depot);

        assertThat(depot.effacerLesArchivesPerimees(LocalDate.of(2034, 1, 1))).isEqualTo(1);
        assertThat(depot.lire("CMD-20260811-A1B2C")).isEmpty();
    }

    @Test
    void n_efface_que_les_factures_echues() throws IOException {
        DepotFacturesArchivees depot = depotSur(dossierTemporaire);
        deposer("CMD-20240115-VIEILLE", depot);
        deposer("CMD-20260811-RECENTE", depot);

        // Début 2032 : celle de 2024 est échue, celle de 2026 non.
        assertThat(depot.effacerLesArchivesPerimees(LocalDate.of(2032, 1, 1))).isEqualTo(1);
        assertThat(depot.lire("CMD-20240115-VIEILLE")).isEmpty();
        assertThat(depot.lire("CMD-20260811-RECENTE")).isPresent();
    }

    @Test
    void laisse_intact_un_fichier_au_nom_inattendu() throws IOException {
        DepotFacturesArchivees depot = depotSur(dossierTemporaire);
        Path intrus = dossierTemporaire.resolve("factures").resolve("note-interne.pdf");
        Files.createDirectories(intrus.getParent());
        Files.write(intrus, "à ne pas toucher".getBytes());

        // Sans date lisible, impossible de savoir si la conservation est échue :
        // supprimer serait pire que garder.
        assertThat(depot.effacerLesArchivesPerimees(LocalDate.of(2099, 1, 1))).isZero();
        assertThat(Files.exists(intrus)).isTrue();
    }

    @Test
    void ne_se_plaint_pas_quand_aucune_facture_n_a_ete_archivee() {
        DepotFacturesArchivees depot = depotSur(dossierTemporaire.resolve("jamais-cree"));

        assertThat(depot.effacerLesArchivesPerimees(LocalDate.now())).isZero();
    }
}
