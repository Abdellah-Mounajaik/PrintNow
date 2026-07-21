package com.printnow.module.shop.service;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Convertit une adresse en coordonnées GPS via Nominatim (OpenStreetMap), gratuit et sans clé.
 * Principe "fail open" : en cas d'erreur réseau ou d'adresse introuvable, on renvoie simplement
 * Optional.empty() — l'imprimerie est quand même créée/mise à jour, juste sans coordonnées.
 */
@Service
public class GeocodingService {

    private RestClient restClient;

    @PostConstruct
    public void init() {
        restClient = RestClient.builder()
                .baseUrl("https://nominatim.openstreetmap.org")
                // Nominatim exige un User-Agent identifiable (politique d'utilisation)
                .defaultHeader("User-Agent", "PrintNow/1.0 (contact@printnow.local)")
                .build();
    }

    /** @return [latitude, longitude] si l'adresse a été localisée, sinon vide. */
    @SuppressWarnings("unchecked")
    public Optional<double[]> geocode(String query) {
        if (query == null || query.isBlank()) {
            return Optional.empty();
        }
        try {
            List<Map<String, Object>> results = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search")
                            .queryParam("q", query)
                            .queryParam("format", "json")
                            .queryParam("limit", "1")
                            .build())
                    .retrieve()
                    .body(List.class);

            if (results == null || results.isEmpty()) {
                return Optional.empty();
            }

            Map<String, Object> first = results.get(0);
            double lat = Double.parseDouble((String) first.get("lat"));
            double lon = Double.parseDouble((String) first.get("lon"));
            return Optional.of(new double[]{lat, lon});
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
