package com.printnow.module.shop.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

/**
 * Upload public du logo d'une imprimerie, utilisé pendant l'inscription partenaire
 * (avant même que le compte/l'imprimerie n'existe en base).
 */
@RestController
@RequestMapping("/api/uploads")
public class LogoUploadController {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadLogo(@RequestParam("fichier") MultipartFile fichier) {
        if (fichier.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le logo est obligatoire.");
        }
        String contentType = fichier.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le logo doit être une image (PNG, JPG…).");
        }

        try {
            Path dir = Paths.get(uploadDir, "logos");
            Files.createDirectories(dir);

            String extension = "";
            String originalName = fichier.getOriginalFilename();
            if (originalName != null && originalName.contains(".")) {
                extension = originalName.substring(originalName.lastIndexOf('.'));
            }
            String filename = UUID.randomUUID() + extension;

            Files.copy(fichier.getInputStream(), dir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);

            String url = "/uploads/logos/" + filename;
            return ResponseEntity.ok(Map.of("url", url));
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de l'upload du logo", e);
        }
    }
}
