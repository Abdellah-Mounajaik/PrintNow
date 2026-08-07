package com.printnow.module.correction.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limite le nombre d'analyses orthographiques par client et par heure.
 *
 * L'analyse est gratuite alors qu'elle mobilise LanguageTool et le modèle de
 * langue, dont le quota est limité. Une fois ce quota épuisé, l'analyse est
 * refusée pour tout le monde : quelques boucles suffiraient donc à priver les
 * vrais clients de la fonctionnalité.
 *
 * Le compteur porte sur l'identifiant du client, l'endpoint étant authentifié.
 *
 * Implémentation volontairement simple, calquée sur celle du chatbot : fenêtre
 * fixe en mémoire, suffisante pour une instance unique. Une architecture
 * multi-instances demanderait un compteur partagé (Redis par exemple).
 */
@Component
public class QuotaAnalyse {

    /** Au-delà, on purge les entrées expirées pour éviter que la table grossisse sans fin. */
    private static final int SEUIL_PURGE = 1_000;

    @Value("${correction.analyses.par.heure:20}")
    private int maxParHeure;

    private static final long FENETRE_MS = 3_600_000L;

    private static class Fenetre {
        long debut;
        int compteur;

        Fenetre(long debut) {
            this.debut = debut;
            this.compteur = 1;
        }
    }

    private final Map<Long, Fenetre> fenetres = new ConcurrentHashMap<>();

    /** @return true si l'analyse est autorisée, false si le quota est dépassé. */
    public boolean autoriser(Long idClient) {
        if (idClient == null) return true;
        long maintenant = System.currentTimeMillis();

        if (fenetres.size() > SEUIL_PURGE) {
            fenetres.entrySet().removeIf(entree -> maintenant - entree.getValue().debut > FENETRE_MS);
        }

        Fenetre fenetre = fenetres.compute(idClient, (ignore, existante) -> {
            if (existante == null || maintenant - existante.debut > FENETRE_MS) {
                return new Fenetre(maintenant);
            }
            existante.compteur++;
            return existante;
        });

        return fenetre.compteur <= maxParHeure;
    }
}
