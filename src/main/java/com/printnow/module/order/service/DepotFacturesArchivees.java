package com.printnow.module.order.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Range, relit et purge les factures figées sur disque.
 *
 * Isolé de la génération pour que celle-ci puisse relire une archive sans
 * dépendre du service qui l'écrit — les deux se référenceraient mutuellement.
 */
@Component
@Slf4j
public class DepotFacturesArchivees {

    /** Durée de conservation obligatoire des factures en Belgique. */
    private static final int ANNEES_DE_CONSERVATION = 7;

    /** « CMD-20260811-A1B2C » : la date d'émission est dans le numéro. */
    private static final Pattern NUMERO_DATE = Pattern.compile("^CMD-(\\d{8})-");

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

    /**
     * Efface les factures dont la durée de conservation légale est écoulée.
     *
     * Le délai belge ne court pas depuis la date de la facture mais depuis le
     * 1er janvier suivant : une facture de 2026 se conserve jusqu'au
     * 31 décembre 2033. La date est lue dans le numéro de commande
     * (« CMD-20260811-… »), qui est celle de la facture.
     *
     * @return le nombre de fichiers effacés
     */
    public int effacerLesArchivesPerimees(LocalDate aujourdhui) {
        Path dossier = Paths.get(archiveDir, "factures");
        if (!Files.isDirectory(dossier)) return 0;

        int effacees = 0;
        try (Stream<Path> fichiers = Files.list(dossier)) {
            for (Path fichier : fichiers.toList()) {
                LocalDate emission = dateDeFacture(fichier.getFileName().toString());
                if (emission == null) continue; // nom inattendu : on n'y touche pas

                LocalDate finDeConservation = LocalDate.of(emission.getYear() + ANNEES_DE_CONSERVATION, 12, 31);
                if (aujourdhui.isAfter(finDeConservation)) {
                    Files.delete(fichier);
                    log.info("Facture archivée {} effacée : conservation obligatoire échue le {}",
                            fichier.getFileName(), finDeConservation);
                    effacees++;
                }
            }
        } catch (Exception e) {
            log.error("Purge des factures archivées interrompue", e);
        }
        return effacees;
    }

    /** Date portée par un nom de fichier « CMD-yyyyMMdd-XXXXX.pdf », ou null. */
    private LocalDate dateDeFacture(String nomFichier) {
        Matcher m = NUMERO_DATE.matcher(nomFichier);
        if (!m.find()) return null;
        try {
            return LocalDate.parse(m.group(1), DateTimeFormatter.BASIC_ISO_DATE);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private Path cheminDe(String numeroCommande) {
        return Paths.get(archiveDir, "factures", numeroCommande + ".pdf");
    }
}
