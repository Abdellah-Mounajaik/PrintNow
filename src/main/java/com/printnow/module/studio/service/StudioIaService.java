package com.printnow.module.studio.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Demande à Mistral de transformer un brief libre en contenu structuré (JSON).
 *
 * Générique : la consigne (le schéma JSON attendu) est fournie par chaque
 * {@link com.printnow.module.studio.service.gabarit.Gabarit}. Ce service ne sait
 * rien du CV, du flyer ou de la carte — il ne fait qu'appeler le modèle et
 * rendre du JSON propre. Même patron que {@code CorrecteurIaService}.
 */
@Service
@Slf4j
public class StudioIaService {

    @Value("${mistral.api.key}")
    private String apiKey;

    @Value("${mistral.base.url}")
    private String baseUrl;

    @Value("${studio.model:mistral-large-latest}")
    private String model;

    /** Levée si Mistral n'a pas pu être consulté (clé absente, réseau, quota). */
    public static class GenerationIndisponible extends RuntimeException {
        public GenerationIndisponible(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private RestClient restClient;

    @PostConstruct
    public void init() {
        restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public boolean estActif() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * @param promptSysteme la consigne du gabarit (schéma JSON à produire)
     * @param brief         la description libre du client
     * @return le JSON brut (nettoyé des éventuelles balises Markdown)
     * @throws GenerationIndisponible si le modèle n'a pas pu être consulté
     */
    @SuppressWarnings("unchecked")
    public String genererJson(String promptSysteme, String brief) {
        if (!estActif()) {
            throw new GenerationIndisponible("Aucune clé d'API n'est configurée pour Mistral", null);
        }

        Map<String, Object> body = Map.of(
                "model", model,
                "temperature", 0.4,
                "max_tokens", 2000,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", promptSysteme),
                        Map.of("role", "user", "content", brief)
                )
        );

        try {
            Map<String, Object> reponse = restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            List<Map<String, Object>> choix = reponse == null ? null
                    : (List<Map<String, Object>>) reponse.get("choices");
            if (choix == null || choix.isEmpty()) {
                throw new GenerationIndisponible("Réponse vide de Mistral", null);
            }
            Map<String, Object> message = (Map<String, Object>) choix.get(0).get("message");
            String contenu = message == null ? null : (String) message.get("content");
            if (contenu == null || contenu.isBlank()) {
                throw new GenerationIndisponible("Réponse sans contenu de Mistral", null);
            }
            return nettoyerJson(contenu);
        } catch (GenerationIndisponible e) {
            throw e;
        } catch (Exception e) {
            throw new GenerationIndisponible("Mistral n'a pas répondu", e);
        }
    }

    /** Retire une éventuelle enveloppe ```json … ``` que le modèle ajoute parfois. */
    private String nettoyerJson(String contenu) {
        String t = contenu.trim();
        if (t.startsWith("```")) {
            t = t.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
        }
        return t.trim();
    }
}
