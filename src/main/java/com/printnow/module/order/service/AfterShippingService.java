package com.printnow.module.order.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Appels vers l'API AfterShipping.
 * AfterShipping est un agrégateur de suivi : on lui donne un numéro bpost,
 * il interroge bpost et nous renvoie le statut en temps réel.
 *
 * Doc API : https://www.aftership.com/docs/api/4/trackings
 */
@Service
public class AfterShippingService {

    @Value("${aftershipping.api.key}")
    private String apiKey;

    @Value("${aftershipping.base.url}")
    private String baseUrl;

    @Value("${aftershipping.courier.slug}")
    private String courierSlug;

    private RestClient restClient;

    @PostConstruct
    public void init() {
        restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("aftership-api-key", apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * Enregistre un numéro de suivi auprès d'AfterShipping.
     * À appeler une seule fois quand l'imprimeur saisit le numéro.
     */
    public void creerSuivi(String trackingNumber) {
        Map<String, Object> body = Map.of(
                "tracking", Map.of(
                        "slug", courierSlug,
                        "tracking_number", trackingNumber
                )
        );
        restClient.post()
                .uri("/trackings")
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    /**
     * Récupère le tag de statut AfterShipping pour un numéro donné.
     * Tags possibles : Pending, InfoReceived, InTransit, OutForDelivery,
     *                  AttemptFail, Delivered, Exception
     */
    @SuppressWarnings("unchecked")
    public String getTagStatut(String trackingNumber) {
        try {
            Map<String, Object> response = restClient.get()
                    .uri("/trackings/{slug}/{trackingNumber}", courierSlug, trackingNumber)
                    .retrieve()
                    .body(Map.class);

            if (response == null) return null;
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            if (data == null) return null;
            Map<String, Object> tracking = (Map<String, Object>) data.get("tracking");
            if (tracking == null) return null;
            return (String) tracking.get("tag");
        } catch (Exception e) {
            return null;
        }
    }
}
