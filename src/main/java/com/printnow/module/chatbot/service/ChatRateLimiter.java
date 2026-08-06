package com.printnow.module.chatbot.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limite le nombre d'appels au chatbot par adresse IP.
 *
 * L'endpoint est public et chaque appel consomme des tokens Mistral facturés :
 * sans cette limite, n'importe qui pourrait épuiser le quota en boucle.
 *
 * Implémentation volontairement simple (fenêtre fixe en mémoire) : suffisant
 * pour une instance unique. Une architecture multi-instances nécessiterait un
 * compteur partagé (Redis par exemple).
 */
@Component
public class ChatRateLimiter {

    private static final int MAX_REQUETES = 15;
    private static final long FENETRE_MS = 60_000L;
    /** Au-delà, on purge les entrées expirées pour éviter que la map grossisse sans fin. */
    private static final int SEUIL_PURGE = 1_000;

    private static class Fenetre {
        long debut;
        int compteur;

        Fenetre(long debut) {
            this.debut = debut;
            this.compteur = 1;
        }
    }

    private final Map<String, Fenetre> fenetres = new ConcurrentHashMap<>();

    /** @return true si la requête est autorisée, false si le quota est dépassé. */
    public boolean autoriser(String cle) {
        long maintenant = System.currentTimeMillis();

        if (fenetres.size() > SEUIL_PURGE) {
            fenetres.entrySet().removeIf(entry -> maintenant - entry.getValue().debut > FENETRE_MS);
        }

        Fenetre fenetre = fenetres.compute(cle, (ignore, existante) -> {
            if (existante == null || maintenant - existante.debut > FENETRE_MS) {
                return new Fenetre(maintenant);
            }
            existante.compteur++;
            return existante;
        });

        return fenetre.compteur <= MAX_REQUETES;
    }
}
