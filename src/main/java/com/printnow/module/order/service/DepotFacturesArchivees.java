package com.printnow.module.order.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Range et relit les factures figées sur disque.
 *
 * Isolé de la génération pour que celle-ci puisse relire une archive sans
 * dépendre du service qui l'écrit — les deux se référenceraient mutuellement.
 */
@Component
@Slf4j
public class DepotFacturesArchivees {

    /**
     * Volontairement hors du dossier des téléversements : celui-ci est exposé
     * en HTTP, et ces factures nomment leur destinataire. Rien ici n'a à être
     * servi au navigateur.
     */
    @Value("${app.archive.dir:archives}")
    private String archiveDir;

    public boolean existe(String numeroCommande) {
        return Files.exists(cheminDe(numeroCommande));
    }

    /** Relit la facture figée d'une commande, si elle en a une. */
    public Optional<byte[]> lire(String numeroCommande) {
        Path source = cheminDe(numeroCommande);
        try {
            return Files.exists(source) ? Optional.of(Files.readAllBytes(source)) : Optional.empty();
        } catch (Exception e) {
            log.error("Facture archivée de la commande {} illisible", numeroCommande, e);
            return Optional.empty();
        }
    }

    public void ecrire(String numeroCommande, byte[] pdf) throws java.io.IOException {
        Path destination = cheminDe(numeroCommande);
        Files.createDirectories(destination.getParent());
        Files.write(destination, pdf);
    }

    private Path cheminDe(String numeroCommande) {
        return Paths.get(archiveDir, "factures", numeroCommande + ".pdf");
    }
}
