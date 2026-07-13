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
        Tu es un modérateur de commentaires pour une plateforme d'impression.
        On te donne le commentaire d'un client. Réponds uniquement par OUI ou NON, rien d'autre.

        Réponds OUI si le commentaire contient au moins un de ces éléments :
        - une insulte ou un mot dégradant visant une PERSONNE (employé, gérant, vendeur, accueil),
          même léger : bête, moche, abruti, imbécile, incapable, nul, con, idiot, bouffon, etc.
        - une injure ou un propos raciste, homophobe, sexiste, haineux ou sexuel
        - un gros mot ou une expression vulgaire, MÊME s'il vise le service et non une personne
          (ex : à chier, merde, putain, chiant, foutu, dégueulasse)
        - une insulte déguisée : abréviations (tg, fdp, ntm, pd), fautes ou chiffres à la place
          des lettres (c0nnard, conard, b0uff0n)
        - du harcèlement, une menace ou une remarque humiliante

        Réponds NON si la critique est négative mais PROPRE (sans gros mot ni attaque de personne),
        même très sévère : trop cher, service lent, mauvaise qualité, impression floue, décevant,
        à éviter, catastrophique, une honte, très mauvais.

        Exemples :
        le vendeur est un abruti -> OUI
        service à chier -> OUI
        c'est de la merde ce service -> OUI
        la personne à l'accueil est moche et bête -> OUI
        tg bande de fdp -> OUI
        service catastrophique, à éviter absolument -> NON
        trop cher et lent -> NON
        impression floue, très déçu -> NON
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
