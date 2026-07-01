package com.printnow.module.order.service;

import com.printnow.module.order.dto.FichierPDFResponseDTO;
import com.printnow.module.order.model.FichierPDF;
import com.printnow.module.order.model.LigneCommande;
import com.printnow.module.order.repository.FichierPDFRepository;
import com.printnow.module.order.repository.LigneCommandeRepository;
import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
public class FichierPDFService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private final FichierPDFRepository fichierPDFRepository;
    private final LigneCommandeRepository ligneCommandeRepository;

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

            FichierPDF fichier = FichierPDF.builder()
                    .nomFichier(originalName)
                    .cheminStockage(destination.toAbsolutePath().toString())
                    .tailleOctets(file.getSize())
                    .nbPagesDetectees(nbPages)
                    .dateUpload(LocalDateTime.now())
                    .ligneCommande(ligne)
                    .build();

            return toDto(fichierPDFRepository.save(fichier));
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de l'upload du fichier PDF", e);
        }
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
