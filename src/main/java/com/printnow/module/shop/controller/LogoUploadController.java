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

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Upload public du logo d'une imprimerie, utilisé pendant l'inscription partenaire
 * (avant même que le compte/l'imprimerie n'existe en base).
 *
 * Cette route est nécessairement ouverte à tous : elle sert avant qu'un compte
 * n'existe. Ce qui est déposé ici finit servi publiquement depuis
 * /uploads/logos/**, d'où deux précautions.
 *
 * D'abord, le type du fichier est déduit de son contenu et non de ce que le
 * navigateur annonce. L'en-tête Content-Type et le nom du fichier viennent tous
 * deux du client : un fichier .svg ou .html présenté comme « image/png » était
 * accepté, stocké avec son extension d'origine, puis servi depuis le domaine du
 * site — c'est-à-dire du script exécuté chez les visiteurs.
 *
 * Ensuite, la taille est bornée bien plus bas que la limite globale de 50 Mo :
 * un logo pèse quelques dizaines de kilo-octets, et personne n'a besoin d'être
 * connecté pour en déposer.
 */
@RestController
@RequestMapping("/api/uploads")
public class LogoUploadController {

    private static final long TAILLE_MAXIMALE = 2 * 1024 * 1024; // 2 Mo

    /** Formats réellement décodables, et l'extension qu'on leur donne. */
    private static final Map<String, String> EXTENSIONS_PAR_FORMAT = Map.of(
            "png", ".png",
            "jpeg", ".jpg",
            "jpg", ".jpg",
            "gif", ".gif");

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadLogo(@RequestParam("fichier") MultipartFile fichier) {
        if (fichier.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le logo est obligatoire.");
        }
        if (fichier.getSize() > TAILLE_MAXIMALE) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "Le logo ne doit pas dépasser 2 Mo.");
        }

        String extension = EXTENSIONS_PAR_FORMAT.get(formatReel(fichier));

        try {
            Path dir = Paths.get(uploadDir, "logos");
            Files.createDirectories(dir);

            String filename = UUID.randomUUID() + extension;
            Files.copy(fichier.getInputStream(), dir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);

            return ResponseEntity.ok(Map.of("url", "/uploads/logos/" + filename));
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de l'upload du logo", e);
        }
    }

    /**
     * Lit les premiers octets pour reconnaître l'image, sans rien croire de ce
     * que le client annonce.
     *
     * @return le format en minuscules, garanti présent dans la liste acceptée
     * @throws ResponseStatusException 400 si ce n'est pas une image reconnue
     */
    private String formatReel(MultipartFile fichier) {
        try (ImageInputStream flux = ImageIO.createImageInputStream(fichier.getInputStream())) {
            Iterator<ImageReader> lecteurs = flux == null ? null : ImageIO.getImageReaders(flux);
            if (lecteurs == null || !lecteurs.hasNext()) {
                throw refuser();
            }
            String format = lecteurs.next().getFormatName().toLowerCase();
            if (!EXTENSIONS_PAR_FORMAT.containsKey(format)) {
                throw refuser();
            }
            return format;
        } catch (IOException e) {
            throw refuser();
        }
    }

    private ResponseStatusException refuser() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Le logo doit être une image PNG, JPG ou GIF.");
    }
}
