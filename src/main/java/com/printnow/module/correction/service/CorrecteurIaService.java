package com.printnow.module.correction.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Relecture du document par un modèle de langue.
 *
 * LanguageTool applique des règles : il ne voit pas les fautes portant sur un
 * mot par ailleurs correct, et se trompe sur les accords dont le sujet est
 * éloigné (« les enseignants nous ont demandé d'être prudent »). Un modèle de
 * langue, lui, s'appuie sur le sens.
 *
 * En contrepartie il peut vouloir réécrire le texte. Ses propositions ne sont
 * donc jamais appliquées telles quelles : elles passent les contrôles de cette
 * classe, puis ceux de {@link CorrectionService}.
 */
@Service
@Slf4j
public class CorrecteurIaService {

    @Value("${mistral.api.key}")
    private String apiKey;

    @Value("${mistral.base.url}")
    private String baseUrl;

    @Value("${mistral.model.correction}")
    private String model;

    /** Taille maximale d'un envoi ; les documents plus longs sont découpés. */
    private static final int TAILLE_MORCEAU = 12_000;

    /** Garde-fou : un document ne peut pas produire un nombre aberrant de corrections. */
    private static final int MAX_CORRECTIONS = 300;

    private static final String PROMPT = """
        Tu corriges les fautes d'orthographe et d'accord d'un texte français.

        Règles impératives :
        - Ne corrige QUE les fautes d'orthographe, de conjugaison et d'accord.
        - Ne reformule rien, ne change aucun mot correct, ne touche pas au style.
        - Ne corrige jamais un nom propre.
        - Réponds uniquement par des lignes « mot_fautif -> correction », une par faute.
        - N'écris pas de ligne si le mot est déjà correct.
        - Si le texte ne contient aucune faute, réponds AUCUNE.
        """;

    private static final Pattern LIGNE_CORRECTION =
            Pattern.compile("^\\s*[-*\\d.)]*\\s*(.+?)\\s*(?:->|→)\\s*(.+?)\\s*$");

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
     * Levée lorsque la relecture n'a pas pu aboutir : clé absente, quota
     * dépassé, coupure réseau.
     *
     * L'analyse ne doit surtout pas continuer sans elle. Sans le modèle, les
     * mesures montrent que la correction tombe de 57 à 46 justes sur 58, et
     * surtout qu'elle produit des contresens : « connection » devient
     * « confection », « sérieu » devient « série ». Livrer cela contre 2,90 €
     * serait pire que de ne rien livrer du tout.
     */
    public static class RelectureIndisponible extends RuntimeException {
        public RelectureIndisponible(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Soumet le texte au modèle et retourne les corrections retenues.
     *
     * @return mot fautif → correction proposée
     * @throws RelectureIndisponible si le modèle n'a pas pu être consulté
     */
    public Map<String, String> proposer(String texte) {
        Map<String, String> corrections = new LinkedHashMap<>();
        if (texte == null || texte.isBlank()) return corrections;
        if (!estActif()) {
            throw new RelectureIndisponible("Aucune clé d'API n'est configurée pour le modèle de langue", null);
        }

        for (String morceau : decouper(texte)) {
            try {
                lireReponse(interroger(morceau), corrections);
            } catch (Exception e) {
                throw new RelectureIndisponible("Le modèle de langue n'a pas répondu", e);
            }
        }
        return corrections;
    }

    @SuppressWarnings("unchecked")
    private String interroger(String texte) {
        Map<String, Object> body = Map.of(
                "model", model,
                "temperature", 0,
                "max_tokens", 2000,
                "messages", List.of(
                        Map.of("role", "system", "content", PROMPT),
                        Map.of("role", "user", "content", texte)
                )
        );

        Map<String, Object> reponse = restClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .body(body)
                .retrieve()
                .body(Map.class);

        if (reponse == null) return null;
        List<Map<String, Object>> choix = (List<Map<String, Object>>) reponse.get("choices");
        if (choix == null || choix.isEmpty()) return null;
        Map<String, Object> message = (Map<String, Object>) choix.get(0).get("message");
        return message == null ? null : (String) message.get("content");
    }

    /** Lit les lignes « mot -> correction » en écartant celles qui ne conviennent pas. */
    private void lireReponse(String reponse, Map<String, String> corrections) {
        if (reponse == null) return;

        for (String ligne : reponse.split("\\R")) {
            if (corrections.size() >= MAX_CORRECTIONS) return;

            Matcher lecteur = LIGNE_CORRECTION.matcher(ligne);
            if (!lecteur.matches()) continue;

            String motFautif = nettoyer(lecteur.group(1));
            String correction = nettoyer(lecteur.group(2));
            if (acceptable(motFautif, correction)) {
                corrections.putIfAbsent(motFautif, correction);
            }
        }
    }

    /**
     * Une proposition n'est retenue que si elle corrige vraiment un mot, sans
     * réécrire la phrase : le modèle produit parfois des lignes sans effet
     * (« écureuils -> écureuils ») ou des reformulations.
     */
    private boolean acceptable(String motFautif, String correction) {
        if (motFautif.isBlank() || correction.isBlank()) return false;
        if (motFautif.equalsIgnoreCase(correction)) return false;
        // Un mot fautif est un mot : les groupes relèvent de la reformulation.
        if (motFautif.contains(" ") || correction.contains(" ")) return false;
        // Une correction reste proche du mot écrit ; un mot très différent
        // trahit une réécriture plutôt qu'une faute de frappe.
        return Math.abs(motFautif.length() - correction.length()) <= 4;
    }

    private String nettoyer(String texte) {
        return texte.replaceAll("^[\\s\"'«»`*]+|[\\s\"'«»`*.,;:]+$", "").trim();
    }

    /** Découpe un texte trop long en respectant les fins de phrase. */
    private List<String> decouper(String texte) {
        List<String> morceaux = new ArrayList<>();
        if (texte.length() <= TAILLE_MORCEAU) {
            morceaux.add(texte);
            return morceaux;
        }

        int debut = 0;
        while (debut < texte.length()) {
            int fin = Math.min(debut + TAILLE_MORCEAU, texte.length());
            if (fin < texte.length()) {
                int coupure = texte.lastIndexOf('.', fin);
                if (coupure > debut) fin = coupure + 1;
            }
            morceaux.add(texte.substring(debut, fin));
            debut = fin;
        }
        return morceaux;
    }
}
