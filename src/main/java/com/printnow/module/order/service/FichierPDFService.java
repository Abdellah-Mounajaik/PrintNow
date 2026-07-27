package com.printnow.module.order.service;

import com.printnow.module.order.dto.FichierPDFResponseDTO;
import com.printnow.module.order.model.FichierPDF;
import com.printnow.module.order.model.LigneCommande;
import com.printnow.module.order.repository.FichierPDFRepository;
import com.printnow.module.order.repository.LigneCommandeRepository;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FichierPDFService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private final FichierPDFRepository fichierPDFRepository;
    private final LigneCommandeRepository ligneCommandeRepository;

    // Dimensions réelles (mm) des formats proposés — mêmes valeurs que côté
    // frontend (Order.tsx), pour détecter les incohérences entre le fichier
    // déposé et le produit choisi (ex: un CV en format "Cartes de visite").
    private static final Map<String, double[]> FORMAT_DIMENSIONS_MM = Map.ofEntries(
            Map.entry("A6", new double[]{105, 148}),
            Map.entry("A5", new double[]{148, 210}),
            Map.entry("A4", new double[]{210, 297}),
            Map.entry("A3", new double[]{297, 420}),
            Map.entry("A2", new double[]{420, 594}),
            Map.entry("A1", new double[]{594, 841}),
            Map.entry("A0", new double[]{841, 1189}),
            Map.entry("DL_10x21", new double[]{100, 210}),
            Map.entry("CARTE_VISITE_85x55", new double[]{85, 55})
    );
    private static final double FORMAT_TOLERANCE_MM = 5;
    private static final double PT_TO_MM = 25.4 / 72;

    @Transactional
    public FichierPDFResponseDTO uploadFichier(Long ligneCommandeId, MultipartFile file, Integer nbPages) {
        LigneCommande ligne = ligneCommandeRepository.findById(ligneCommandeId)
                .orElseThrow(() -> new RuntimeException("Ligne commande non trouvée : " + ligneCommandeId));

        try {
            Path dir = Paths.get(uploadDir, "commandes",
                    ligne.getCommande().getId().toString(),
                    "lignes", ligneCommandeId.toString());
            Files.createDirectories(dir);

            String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "fichier.pdf";
            String storedName = System.currentTimeMillis() + "_" + originalName;
            Path destination = dir.resolve(storedName);
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

            Integer nbPagesReelles = nbPages;
            try (PDDocument document = Loader.loadPDF(destination.toFile())) {
                nbPagesReelles = document.getNumberOfPages();

                String formatProduit = ligne.getProduit().getFormatImpression() != null
                        ? ligne.getProduit().getFormatImpression().name()
                        : null;
                double[] dimsAttendues = formatProduit != null ? FORMAT_DIMENSIONS_MM.get(formatProduit) : null;

                if (dimsAttendues != null && document.getNumberOfPages() > 0) {
                    PDRectangle box = document.getPage(0).getMediaBox();
                    double largeurMm = Math.abs(box.getWidth()) * PT_TO_MM;
                    double hauteurMm = Math.abs(box.getHeight()) * PT_TO_MM;

                    if (!formatCorrespond(dimsAttendues, largeurMm, hauteurMm)) {
                        Files.deleteIfExists(destination);
                        throw new IllegalArgumentException(String.format(
                                "Le format du fichier (%.0f×%.0f mm) ne correspond pas au produit sélectionné (%.0f×%.0f mm).",
                                largeurMm, hauteurMm, dimsAttendues[0], dimsAttendues[1]));
                    }
                }
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                // PDF illisible par PDFBox (chiffré, corrompu...) : on ne bloque pas,
                // on garde simplement le nombre de pages annoncé par le client.
            }

            FichierPDF fichier = FichierPDF.builder()
                    .nomFichier(originalName)
                    .cheminStockage(destination.toAbsolutePath().toString())
                    .tailleOctets(file.getSize())
                    .nbPagesDetectees(nbPagesReelles != null ? nbPagesReelles : nbPages)
                    .dateUpload(LocalDateTime.now())
                    .ligneCommande(ligne)
                    .build();

            return toDto(fichierPDFRepository.save(fichier));
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de l'upload du fichier PDF", e);
        }
    }

    private boolean formatCorrespond(double[] dimsAttendues, double largeurMm, double hauteurMm) {
        return correspondOrientation(dimsAttendues[0], dimsAttendues[1], largeurMm, hauteurMm)
                || correspondOrientation(dimsAttendues[1], dimsAttendues[0], largeurMm, hauteurMm);
    }

    private boolean correspondOrientation(double largeurAttendue, double hauteurAttendue, double largeurMm, double hauteurMm) {
        return Math.abs(largeurMm - largeurAttendue) <= FORMAT_TOLERANCE_MM
                && Math.abs(hauteurMm - hauteurAttendue) <= FORMAT_TOLERANCE_MM;
    }

    public ResponseEntity<Resource> downloadFichier(Long fichierId) {
        FichierPDF fichier = fichierPDFRepository.findById(fichierId)
                .orElseThrow(() -> new RuntimeException("Fichier PDF non trouvé : " + fichierId));
        try {
            Path path = Paths.get(fichier.getCheminStockage());
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists()) throw new RuntimeException("Fichier introuvable sur le serveur");

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fichier.getNomFichier() + "\"")
                    .body(resource);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors du téléchargement", e);
        }
    }

    private FichierPDFResponseDTO toDto(FichierPDF f) {
        FichierPDFResponseDTO dto = new FichierPDFResponseDTO();
        dto.setId(f.getId());
        dto.setNomFichier(f.getNomFichier());
        dto.setTailleOctets(f.getTailleOctets());
        dto.setNbPagesDetectees(f.getNbPagesDetectees());
        dto.setDateUpload(f.getDateUpload());
        return dto;
    }
}
