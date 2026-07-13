package com.printnow.module.avis.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Modération des commentaires d'avis via l'API Mistral (free tier, disponible en UE).
 * On demande à Mistral de classer le commentaire : contient-il une insulte / propos haineux ?
 *
 * Principe "fail open" : si l'API est indisponible ou la clé absente, on laisse passer
 * le commentaire (on ne bloque pas les avis légitimes à cause d'une panne externe).
 */
@Service
public class ModerationService {

    @Value("${mistral.api.key}")
    private String apiKey;

    @Value("${mistral.base.url}")
    private String baseUrl;

    @Value("${mistral.model}")
    private String model;

    private static final String SYSTEM_PROMPT = """
        Tu es un modérateur de commentaires pour une plateforme belge d'impression.
        On te donne le commentaire d'un client sur une imprimerie.
        Réponds uniquement par un seul mot :
        - "OUI" si le commentaire contient une insulte, une injure, du harcèlement,
          un propos raciste, haineux, sexuel ou grossier dirigé contre une personne.
        - "NON" dans tous les autres cas, y compris une critique négative mais correcte
          (ex : "service lent", "mauvaise qualité", "trop cher" ne sont PAS des insultes).
        Ne réponds rien d'autre que OUI ou NON.
        """;

    private RestClient restClient;

    @PostConstruct
    public void init() {
        restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * @return true si le commentaire est inapproprié (insulte détectée), false sinon.
     */
    @SuppressWarnings("unchecked")
    public boolean estInapproprie(String commentaire) {
        if (commentaire == null || commentaire.isBlank() || apiKey == null || apiKey.isBlank()) {
            return false;
        }

        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "max_tokens", 5,
                    "temperature", 0,
                    "messages", List.of(
                            Map.of("role", "system", "content", SYSTEM_PROMPT),
                            Map.of("role", "user", "content", commentaire)
                    )
            );

            Map<String, Object> response = restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            String texte = extraireTexte(response);
            return texte != null && texte.trim().toUpperCase().startsWith("OUI");
        } catch (Exception e) {
            // fail open : en cas d'erreur API, on ne bloque pas l'avis
            return false;
        }
    }

    /** Extrait le texte de la réponse Mistral : choices[0].message.content */
    @SuppressWarnings("unchecked")
    private String extraireTexte(Map<String, Object> response) {
        if (response == null) return null;
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) return null;
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        if (message == null) return null;
        return (String) message.get("content");
    }
}
