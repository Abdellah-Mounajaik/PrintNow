package com.printnow.module.correction.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Suit l'avancement des analyses en cours, pour que le client ne reste pas
 * devant un écran figé.
 *
 * L'analyse demande plusieurs secondes : le texte est extrait, relu par
 * LanguageTool, chaque correction est éprouvée dans sa phrase, puis le modèle de
 * langue apporte sa propre lecture. Chaque étape franchie est publiée ici, et le
 * navigateur vient la consulter pendant qu'il attend la réponse.
 *
 * Le client fournit lui-même l'identifiant de suivi au moment de l'envoi : il
 * peut ainsi interroger l'avancement avant même de connaître le numéro de la
 * vérification, qui n'existe qu'une fois l'analyse terminée.
 *
 * Implémentation volontairement simple, comme le quota : table en mémoire,
 * suffisante pour une instance unique.
 */
@Component
public class ProgressionAnalyse {

    /** Une analyse abandonnée en chemin ne doit pas encombrer la table. */
    private static final long DUREE_VIE_MS = 300_000L;
    private static final int SEUIL_PURGE = 500;

    /**
     * @param pourcentage avancement estimé, de 0 à 100
     * @param libelle     ce qui se passe, en clair
     */
    public record Etape(int pourcentage, String libelle, long instant) {}

    private final Map<String, Etape> etapes = new ConcurrentHashMap<>();

    public void publier(String suivi, int pourcentage, String libelle) {
        if (suivi == null || suivi.isBlank()) return;

        long maintenant = System.currentTimeMillis();
        if (etapes.size() > SEUIL_PURGE) {
            etapes.entrySet().removeIf(entree -> maintenant - entree.getValue().instant() > DUREE_VIE_MS);
        }
        etapes.put(suivi, new Etape(pourcentage, libelle, maintenant));
    }

    /** @return l'étape en cours, ou null si l'analyse est inconnue ou terminée */
    public Etape lire(String suivi) {
        return suivi == null || suivi.isBlank() ? null : etapes.get(suivi);
    }

    public void terminer(String suivi) {
        if (suivi != null && !suivi.isBlank()) etapes.remove(suivi);
    }
}
