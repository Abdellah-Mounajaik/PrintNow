package com.printnow.module.correction.service;

import com.printnow.module.correction.dto.FauteDTO;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Client de l'instance LanguageTool auto-hébergée (voir docker-compose.yml).
 *
 * LanguageTool applique des règles déterministes : contrairement à un modèle de
 * langage, il ne peut pas inventer une correction. C'est la raison pour laquelle
 * il a été retenu pour un service payant.
 */
@Service
@Slf4j
public class LanguageToolClient {

    @Value("${languagetool.base.url}")
    private String baseUrl;

    /**
     * Instance dépourvue de modèle de langue, pour les langues dont nous n'avons
     * pas les n-grammes.
     *
     * Mesuré : lorsque « languageModel » désigne un dossier sans sous-dossier
     * pour la langue demandée, LanguageTool désactive silencieusement toutes ses
     * règles. Sur « This is a beatiful and realy intresting documment »,
     * l'instance française ne signalait rien ; celle-ci relève les cinq fautes.
     */
    @Value("${languagetool.base.url.sans.modele}")
    private String baseUrlSansModele;

    /**
     * Langues pour lesquelles les n-grammes sont installés, et qui passent donc
     * par l'instance principale — la seule à faire jouer les paires de confusion
     * (« une grande foret » → « forêt »).
     */
    @Value("${languagetool.langues.avec.modele}")
    private String languesAvecModele;

    /** Langues proposées au client, du code LanguageTool vers son nom lisible. */
    public static final Map<String, String> LANGUES_PRISES_EN_CHARGE = Map.of(
            "fr", "français",
            "nl", "néerlandais",
            "en-US", "anglais");

    /**
     * LanguageTool refuse les textes trop volumineux en une requête : on découpe
     * le texte d'une page si nécessaire (rare, mais possible sur des pages denses).
     */
    private static final int TAILLE_MAX_REQUETE = 20_000;

    /**
     * Catégories écartées : elles relèvent du style et non de l'orthographe.
     * Le service corrige les fautes, il ne réécrit pas le texte de l'auteur
     * (LanguageTool propose par exemple « débuter » → « entamer »).
     */
    private static final Set<String> CATEGORIES_IGNOREES = Set.of(
            "CAT_TOURS_CRITIQUES",  // tournures jugées critiquables
            "STYLE",
            "REDUNDANCY",
            "REPETITIONS_STYLE",    // propose un synonyme quand un mot se répète
            "COLLOQUIALISMS",
            "TYPOGRAPHY",           // guillemets, espaces fines : pas des fautes
            "PONCTUATION",          // virgules conseillées : affaire de style
            "PONCTUATION_VIRGULE",
            "PUNCTUATION",
            // Anglais : préférences régionales, pas des fautes. « Afterwards »
            // est correct, l'américain préfère seulement « Afterward ».
            "BRITISH_ENGLISH",
            "AMERICAN_ENGLISH_STYLE",
            "REGIONALISMS",
            // Néerlandais : registre et tournures conseillées.
            "STIJL",
            "GEBRUIK"
    );

    private RestClient avecModele;
    private RestClient sansModele;
    private Set<String> languesDuModele;

    @PostConstruct
    public void init() {
        avecModele = RestClient.builder().baseUrl(baseUrl).build();
        sansModele = RestClient.builder().baseUrl(baseUrlSansModele).build();
        languesDuModele = Set.of(languesAvecModele.split("\\s*,\\s*"));
    }

    /** L'instance à interroger pour cette langue. */
    private RestClient clientPour(String langue) {
        return languesDuModele.contains(langue) ? avecModele : sansModele;
    }

    /**
     * Reconnaît la langue du document.
     *
     * Un extrait suffit : l'identification d'une langue ne demande pas le texte
     * entier, et l'envoyer coûterait une analyse complète pour rien.
     *
     * @return le code d'une langue prise en charge, ou null si le document est
     *         rédigé dans une autre langue
     */
    @SuppressWarnings("unchecked")
    public String detecterLangue(String texte) {
        if (texte == null || texte.isBlank()) return null;

        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("language", "auto");
            form.add("text", texte.substring(0, Math.min(texte.length(), 600)));

            Map<String, Object> reponse = sansModele.post()
                    .uri("/v2/check")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);

            if (reponse == null) return null;
            Map<String, Object> langue = (Map<String, Object>) reponse.get("language");
            if (langue == null) return null;
            Map<String, Object> detectee = (Map<String, Object>) langue.get("detectedLanguage");
            String code = String.valueOf((detectee == null ? langue : detectee).get("code"));

            // « en-GB », « nl-BE »… sont ramenés à la variante que nous servons.
            for (String prise : LANGUES_PRISES_EN_CHARGE.keySet()) {
                if (code.regionMatches(true, 0, prise, 0, 2)) return prise;
            }
            return null;

        } catch (Exception e) {
            log.warn("Détection de langue impossible", e);
            return null;
        }
    }

    /**
     * Analyse toutes les pages d'un document en un minimum de requêtes.
     *
     * Une requête coûtant surtout un temps fixe, envoyer les pages une à une
     * multipliait inutilement les allers-retours : un mémoire de cinquante pages
     * demandait cinquante appels. Les pages sont donc regroupées jusqu'à la
     * taille maximale d'un envoi, et chaque faute est rattachée à la sienne
     * d'après la position du signalement.
     *
     * @param textes le texte de chaque page, dans l'ordre
     */
    public List<FauteDTO> analyserPages(List<String> textes, String langue) {
        List<FauteDTO> fautes = new ArrayList<>();
        if (textes == null || textes.isEmpty()) return fautes;

        List<Integer> groupe = new ArrayList<>();
        int longueur = 0;

        for (int i = 0; i < textes.size(); i++) {
            String texte = textes.get(i);
            if (texte == null || texte.isBlank()) continue;

            // Une page à elle seule plus longue qu'un envoi garde son découpage propre.
            if (texte.length() >= TAILLE_MAX_REQUETE) {
                if (!groupe.isEmpty()) {
                    fautes.addAll(analyserGroupe(textes, groupe, langue));
                    groupe = new ArrayList<>();
                    longueur = 0;
                }
                fautes.addAll(analyserPage(texte, i + 1, langue));
                continue;
            }

            int cout = texte.length() + SEPARATEUR_LOT.length();
            if (!groupe.isEmpty() && longueur + cout > TAILLE_MAX_REQUETE) {
                fautes.addAll(analyserGroupe(textes, groupe, langue));
                groupe = new ArrayList<>();
                longueur = 0;
            }
            groupe.add(i);
            longueur += cout;
        }
        if (!groupe.isEmpty()) fautes.addAll(analyserGroupe(textes, groupe, langue));

        return fautes;
    }

    /**
     * Où se situe un signalement dans l'envoi.
     *
     * @param page  numéro de la page concernée
     * @param debut position à laquelle le texte de cette page commence dans
     *              l'envoi ; sert à reconnaître un signalement en tête de page
     */
    private record Reperage(int page, int debut) {}

    /** Envoie un groupe de pages et rattache chaque faute à la sienne. */
    @SuppressWarnings("unchecked")
    private List<FauteDTO> analyserGroupe(List<String> textes, List<Integer> indices, String langue) {
        StringBuilder envoi = new StringBuilder();
        int[] debuts = new int[indices.size()];
        int[] fins = new int[indices.size()];
        for (int rang = 0; rang < indices.size(); rang++) {
            if (rang > 0) envoi.append(SEPARATEUR_LOT);
            debuts[rang] = envoi.length();
            envoi.append(textes.get(indices.get(rang)));
            fins[rang] = envoi.length();
        }

        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("language", langue);
            form.add("text", envoi.toString());

            Map<String, Object> reponse = clientPour(langue).post()
                    .uri("/v2/check")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);

            return extraireFautes(reponse, envoi.toString(), position -> {
                for (int rang = 0; rang < fins.length; rang++) {
                    if (position < fins[rang]) return new Reperage(indices.get(rang) + 1, debuts[rang]);
                }
                int dernier = indices.size() - 1;
                return new Reperage(indices.get(dernier) + 1, debuts[dernier]);
            });

        } catch (Exception e) {
            log.warn("Échec de l'analyse LanguageTool (pages {})", indices, e);
            return List.of();
        }
    }

    /**
     * Analyse le texte d'une seule page.
     *
     * Réservé aux pages trop longues pour tenir dans un envoi groupé : elles
     * sont alors découpées en morceaux successifs.
     *
     * @param texte le texte extrait de la page
     * @param page  le numéro de page (repris tel quel dans les fautes retournées)
     */
    @SuppressWarnings("unchecked")
    public List<FauteDTO> analyserPage(String texte, int page, String langue) {
        List<FauteDTO> fautes = new ArrayList<>();
        if (texte == null || texte.isBlank()) return fautes;

        for (String morceau : decouper(texte)) {
            try {
                MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
                form.add("language", langue);
                form.add("text", morceau);

                Map<String, Object> reponse = clientPour(langue).post()
                        .uri("/v2/check")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body(form)
                        .retrieve()
                        .body(Map.class);

                fautes.addAll(extraireFautes(reponse, morceau, position -> new Reperage(page, 0)));
            } catch (Exception e) {
                log.warn("Échec de l'analyse LanguageTool (page {})", page, e);
            }
        }
        return fautes;
    }

    /**
     * Verdict rendu sur un mot replacé dans sa phrase.
     *
     * @param accepte   true si le correcteur ne trouve plus rien à redire
     * @param aEssayer  la forme qu'il propose à la place, le cas échéant
     */
    public record Verdict(boolean accepte, String aEssayer) {}

    /** Un mot à éprouver, et la phrase dans laquelle le juger. */
    public record Epreuve(String phrase, String mot) {}

    /** Sépare les phrases d'un même envoi ; un saut de paragraphe les isole. */
    private static final String SEPARATEUR_LOT = "\n\n";

    /**
     * Éprouve plusieurs mots, chacun dans sa phrase, en un minimum de requêtes.
     *
     * Une requête coûte presque le même temps qu'elle porte une phrase ou trente
     * — mesuré : 1,19 s pour 122 caractères, 2,00 s pour 3 660. Les mener une à
     * une était donc le principal poste de lenteur de l'analyse. Les phrases sont
     * envoyées séparées par un saut de paragraphe, et chaque signalement est
     * rattaché à la sienne d'après sa position dans l'envoi.
     *
     * @return un verdict par épreuve, dans l'ordre reçu
     */
    public List<Verdict> eprouverEnLot(List<Epreuve> epreuves, String langue) {
        List<Verdict> verdicts = new ArrayList<>(java.util.Collections.nCopies(epreuves.size(), null));

        List<Integer> lot = new ArrayList<>();
        int longueurLot = 0;
        for (int i = 0; i < epreuves.size(); i++) {
            Epreuve epreuve = epreuves.get(i);
            if (epreuve.phrase() == null || epreuve.phrase().isBlank()
                    || epreuve.mot() == null || epreuve.mot().isBlank()) {
                verdicts.set(i, new Verdict(false, null));
                continue;
            }
            int longueur = epreuve.phrase().length() + SEPARATEUR_LOT.length();
            if (!lot.isEmpty() && longueurLot + longueur > TAILLE_MAX_REQUETE) {
                traiterLot(epreuves, lot, verdicts, langue);
                lot = new ArrayList<>();
                longueurLot = 0;
            }
            lot.add(i);
            longueurLot += longueur;
        }
        if (!lot.isEmpty()) traiterLot(epreuves, lot, verdicts, langue);

        return verdicts;
    }

    /** Envoie un groupe de phrases et répartit les signalements entre elles. */
    @SuppressWarnings("unchecked")
    private void traiterLot(List<Epreuve> epreuves, List<Integer> indices, List<Verdict> verdicts, String langue) {
        // Position de chaque phrase dans l'envoi, pour rattacher les signalements.
        StringBuilder envoi = new StringBuilder();
        int[] debuts = new int[indices.size()];
        int[] fins = new int[indices.size()];
        for (int rang = 0; rang < indices.size(); rang++) {
            if (rang > 0) envoi.append(SEPARATEUR_LOT);
            debuts[rang] = envoi.length();
            envoi.append(epreuves.get(indices.get(rang)).phrase());
            fins[rang] = envoi.length();
        }

        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("language", langue);
            form.add("text", envoi.toString());

            Map<String, Object> reponse = clientPour(langue).post()
                    .uri("/v2/check")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);

            List<Map<String, Object>> matches = reponse == null
                    ? null : (List<Map<String, Object>>) reponse.get("matches");

            for (Map<String, Object> match : matches == null ? List.<Map<String, Object>>of() : matches) {
                if (aEcarter(match, 1, 0)) continue;
                if (!(match.get("offset") instanceof Number position)) continue;

                int rang = rangContenant(debuts, fins, position.intValue());
                if (rang < 0) continue;

                int indice = indices.get(rang);
                if (verdicts.get(indice) != null) continue; // premier signalement retenu

                Epreuve epreuve = epreuves.get(indice);
                String signale = spanSignale(match);
                if (signale == null || !signale.contains(epreuve.mot())) continue;

                verdicts.set(indice, new Verdict(false, pisteProposee(match, signale, epreuve.mot())));
            }

            // Sans signalement, le mot est accepté dans sa phrase.
            for (int indice : indices) {
                if (verdicts.get(indice) == null) verdicts.set(indice, new Verdict(true, null));
            }

        } catch (Exception e) {
            log.debug("Épreuve groupée impossible", e);
            for (int indice : indices) {
                if (verdicts.get(indice) == null) verdicts.set(indice, new Verdict(false, null));
            }
        }
    }

    /** À quelle phrase de l'envoi appartient un signalement situé à cette position ? */
    private int rangContenant(int[] debuts, int[] fins, int position) {
        for (int rang = 0; rang < debuts.length; rang++) {
            if (position >= debuts[rang] && position < fins[rang]) return rang;
        }
        return -1;
    }

    /** Texte exact visé par un signalement. */
    @SuppressWarnings("unchecked")
    private String spanSignale(Map<String, Object> match) {
        Map<String, Object> contexte = (Map<String, Object>) match.get("context");
        if (contexte == null) return null;
        String texte = (String) contexte.get("text");
        if (texte == null) return null;
        int debut = ((Number) contexte.get("offset")).intValue();
        int longueur = ((Number) contexte.get("length")).intValue();
        if (debut < 0 || debut + longueur > texte.length()) return null;
        return texte.substring(debut, debut + longueur);
    }

    /** Forme que le correcteur propose à la place du mot éprouvé, s'il en propose une. */
    @SuppressWarnings("unchecked")
    private String pisteProposee(Map<String, Object> match, String signale, String mot) {
        List<Map<String, Object>> remplacements = (List<Map<String, Object>>) match.get("replacements");
        if (remplacements == null || remplacements.isEmpty()) return null;
        String valeur = (String) remplacements.get(0).get("value");
        if (valeur == null || valeur.isBlank()) return null;
        return motCorrespondant(signale, valeur, mot);
    }

    /**
     * Éprouve un mot dans sa phrase.
     *
     * C'est ce qui permet de départager les suggestions : « nettoyer » est bien
     * orthographié, mais « nous avons nettoyer » reste fautif, tandis que
     * « nous avons nettoyé » passe sans encombre. En cas de rejet, le correcteur
     * indique souvent la bonne forme : on la récupère pour l'essayer à son tour.
     */
    @SuppressWarnings("unchecked")
    public Verdict eprouverDansLaPhrase(String phrase, String mot, String langue) {
        if (phrase == null || phrase.isBlank() || mot == null || mot.isBlank()) {
            return new Verdict(false, null);
        }

        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("language", langue);
            form.add("text", phrase);

            Map<String, Object> reponse = clientPour(langue).post()
                    .uri("/v2/check")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);

            if (reponse == null) return new Verdict(false, null);
            List<Map<String, Object>> matches = (List<Map<String, Object>>) reponse.get("matches");
            if (matches == null || matches.isEmpty()) return new Verdict(true, null);

            // Les autres fautes de la phrase ne nous concernent pas : seul compte
            // le fait que le mot proposé soit lui-même signalé.
            for (Map<String, Object> match : matches) {
                if (aEcarter(match, 1, 0)) continue;

                Map<String, Object> contexte = (Map<String, Object>) match.get("context");
                if (contexte == null) continue;
                String texte = (String) contexte.get("text");
                int debut = ((Number) contexte.get("offset")).intValue();
                int longueur = ((Number) contexte.get("length")).intValue();
                if (texte == null || debut < 0 || debut + longueur > texte.length()) continue;

                String signale = texte.substring(debut, debut + longueur);
                if (!signale.contains(mot)) continue;

                // Le correcteur refuse ce mot : sa suggestion devient une piste.
                List<Map<String, Object>> remplacements = (List<Map<String, Object>>) match.get("replacements");
                String propose = null;
                if (remplacements != null && !remplacements.isEmpty()) {
                    String valeur = (String) remplacements.get(0).get("value");
                    // On ne retient que le mot correspondant, pas la reformulation
                    // du groupe (« avons nettoyer » → « avons nettoyé »).
                    if (valeur != null && !valeur.isBlank()) {
                        propose = motCorrespondant(signale, valeur, mot);
                    }
                }
                return new Verdict(false, propose);
            }
            return new Verdict(true, null);

        } catch (Exception e) {
            log.debug("Validation en contexte impossible pour « {} »", mot, e);
            return new Verdict(false, null);
        }
    }

    /**
     * Extrait, d'une correction portant sur plusieurs mots, celui qui remplace
     * le mot éprouvé.
     */
    private String motCorrespondant(String signale, String correction, String mot) {
        String[] avant = signale.trim().split("\\s+");
        String[] apres = correction.trim().split("\\s+");
        if (avant.length != apres.length) return apres.length == 1 ? apres[0] : null;

        for (int i = 0; i < avant.length; i++) {
            if (avant[i].contains(mot) && !avant[i].equals(apres[i])) return apres[i];
        }
        return null;
    }

    /**
     * Le mot est-il inconnu du dictionnaire ?
     *
     * Sert de garde-fou aux propositions du modèle de langue : il ne peut ainsi
     * pas introduire un mot inventé. On ne regarde que l'orthographe pure, les
     * remarques d'accord étant écartées — sur ce terrain, le modèle voit souvent
     * plus juste que les règles.
     */
    @SuppressWarnings("unchecked")
    public boolean estMalOrthographie(String phrase, String mot, String langue) {
        if (phrase == null || phrase.isBlank() || mot == null || mot.isBlank()) return false;

        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("language", langue);
            form.add("text", phrase);

            Map<String, Object> reponse = clientPour(langue).post()
                    .uri("/v2/check")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);

            if (reponse == null) return false;
            List<Map<String, Object>> matches = (List<Map<String, Object>>) reponse.get("matches");
            if (matches == null) return false;

            for (Map<String, Object> match : matches) {
                Map<String, Object> regle = (Map<String, Object>) match.get("rule");
                if (regle == null) continue;
                Map<String, Object> categorie = (Map<String, Object>) regle.get("category");
                boolean orthographe = categorie != null && "TYPOS".equals(String.valueOf(categorie.get("id")));
                if (!orthographe) continue;

                Map<String, Object> contexte = (Map<String, Object>) match.get("context");
                if (contexte == null) continue;
                String texte = (String) contexte.get("text");
                int debut = ((Number) contexte.get("offset")).intValue();
                int longueur = ((Number) contexte.get("length")).intValue();
                if (texte == null || debut < 0 || debut + longueur > texte.length()) continue;

                if (texte.substring(debut, debut + longueur).contains(mot)) return true;
            }
            return false;

        } catch (Exception e) {
            log.debug("Contrôle orthographique impossible pour « {} »", mot, e);
            return false;
        }
    }

    /** Vérifie que le service est joignable (utilisé avant de facturer quoi que ce soit). */
    public boolean estDisponible() {
        try {
            avecModele.get().uri("/v2/languages").retrieve().toBodilessEntity();
            sansModele.get().uri("/v2/languages").retrieve().toBodilessEntity();
            return true;
        } catch (Exception e) {
            log.warn("LanguageTool injoignable sur {}", baseUrl, e);
            return false;
        }
    }

    /**
     * @param reperage situe le signalement d'après sa position dans l'envoi ;
     *        un envoi peut porter plusieurs pages
     */
    /**
     * Rang de l'occurrence signalée parmi celles du même mot dans la page.
     *
     * Compté sur le texte même qui a été analysé, donc exact. Sert à ne réécrire
     * que le mot fautif lorsqu'il figure aussi ailleurs, correctement employé.
     */
    private Integer rangDansLaPage(String envoi, int debutDePage, int position, String mot) {
        if (envoi == null || position < debutDePage || position > envoi.length()) return null;

        Matcher chercheur = Pattern
                .compile("\\b" + Pattern.quote(mot) + "\\b", Pattern.UNICODE_CHARACTER_CLASS)
                .matcher(envoi.substring(debutDePage, position));

        int rang = 1;
        while (chercheur.find()) rang++;
        return rang;
    }

    @SuppressWarnings("unchecked")
    private List<FauteDTO> extraireFautes(Map<String, Object> reponse, String envoi,
                                          IntFunction<Reperage> reperage) {
        List<FauteDTO> fautes = new ArrayList<>();
        if (reponse == null) return fautes;

        List<Map<String, Object>> matches = (List<Map<String, Object>>) reponse.get("matches");
        if (matches == null) return fautes;

        for (Map<String, Object> match : matches) {
            int position = match.get("offset") instanceof Number nombre ? nombre.intValue() : 0;
            Reperage ou = reperage.apply(position);
            int page = ou.page();
            if (aEcarter(match, page, ou.debut())) continue;

            Map<String, Object> contexte = (Map<String, Object>) match.get("context");
            if (contexte == null) continue;

            String texteContexte = (String) contexte.get("text");
            int debut = ((Number) contexte.get("offset")).intValue();
            int longueur = ((Number) contexte.get("length")).intValue();
            if (texteContexte == null || debut < 0 || debut + longueur > texteContexte.length()) continue;

            // Le signalement englobe parfois l'espace précédente (LanguageTool
            // propose « , mais » pour « mais »). On la retire : sans cela, la
            // correction effacerait l'espace et souderait les deux mots.
            String motFautif = texteContexte.substring(debut, debut + longueur).trim();
            if (motFautif.isBlank()) continue;

            List<Map<String, Object>> remplacements = (List<Map<String, Object>>) match.get("replacements");
            List<String> suggestions = classer(motFautif, remplacements);

            // Une correction ne doit toucher qu'un seul mot : au-delà, il ne
            // s'agit plus d'orthographe mais d'une reformulation de la phrase
            // (« des paysages magnifiques et appris » → « de magnifiques paysages appris »).
            List<String> retenues = suggestions.stream().filter(s -> unSeulMotChange(motFautif, s)).toList();

            if (retenues.isEmpty() && !motFautif.contains(" ")) {
                // Un mot isolé n'a parfois pour seule piste qu'un découpage :
                // « clairesse » → « claires se ». Le mot recherché est alors l'un
                // des fragments (« claires »), qu'on récupère comme candidat ;
                // la validation en contexte se chargera de trancher.
                final String reference = motFautif;
                retenues = fragments(suggestions).stream()
                        .filter(f -> !f.equals(reference))
                        .toList();
            }
            suggestions = retenues;

            // Les signalements portant sur un groupe de mots sans correction
            // exploitable relèvent d'une reformulation : on les écarte.
            if (suggestions.isEmpty() && motFautif.trim().contains(" ")) continue;

            fautes.add(new FauteDTO(
                    page,
                    motFautif,
                    suggestions.isEmpty() ? null : suggestions.get(0),
                    suggestions,
                    (String) match.get("message"),
                    texteContexte,
                    rangDansLaPage(envoi, ou.debut(), position, motFautif),
                    false // renseigné plus tard, lors de la génération du PDF corrigé
            ));
        }
        return fautes;
    }

    /**
     * Reclasse les suggestions de LanguageTool.
     *
     * Sans données statistiques de contexte, LanguageTool classe parfois une
     * forme éloignée en tête : pour « texe » il propose « t'axe » avant
     * « texte ». On retient donc d'abord la suggestion demandant le moins de
     * modifications, en écartant celles qui coupent le mot en deux (« souvenires »
     * → « souvenir es ») : une espace ajoutée n'est presque jamais l'intention.
     * À égalité, l'ordre de LanguageTool fait foi.
     */
    @SuppressWarnings("unchecked")
    private List<String> classer(String motFautif, List<Map<String, Object>> remplacements) {
        if (remplacements == null || remplacements.isEmpty()) return List.of();

        List<String> valeurs = new ArrayList<>();
        for (Map<String, Object> remplacement : remplacements) {
            String valeur = (String) remplacement.get("value");
            if (valeur == null || valeur.isBlank()) continue;
            // Une suggestion qui ne diffère du mot écrit que par une espace ne
            // corrige rien, et l'appliquer souderait les mots voisins.
            if (valeur.trim().equals(motFautif.trim())) continue;
            valeurs.add(valeur.trim());
        }
        if (valeurs.size() <= 1) return valeurs;

        String reference = motFautif.toLowerCase();
        int espacesOrigine = compterEspaces(reference);

        List<String> classees = new ArrayList<>(valeurs);
        classees.sort(Comparator
                // 1. écarte le découpage du mot en plusieurs morceaux
                .comparing((String s) -> compterEspaces(s.toLowerCase()) > espacesOrigine ? 1 : 0)
                // 2. la suggestion demandant le moins de modifications
                .thenComparing(s -> distance(reference, s.toLowerCase()))
                // 3. à égalité, on conserve l'ordre de LanguageTool
                .thenComparing(valeurs::indexOf));
        return classees;
    }

    /**
     * Écarte les signalements qui ne relèvent pas de l'orthographe.
     */
    @SuppressWarnings("unchecked")
    private boolean aEcarter(Map<String, Object> match, int page, int debutDePage) {
        Map<String, Object> regle = (Map<String, Object>) match.get("rule");
        if (regle == null) return true;

        Map<String, Object> categorie = (Map<String, Object>) regle.get("category");
        if (categorie != null && CATEGORIES_IGNOREES.contains(String.valueOf(categorie.get("id")))) {
            return true;
        }

        // Une page qui reprend une phrase commencée à la précédente déclenche à
        // tort la règle de majuscule initiale. La comparaison porte sur le début
        // de la page, et non de l'envoi : plusieurs pages voyagent ensemble.
        String identifiant = String.valueOf(regle.get("id"));
        if (page > 1 && identifiant.contains("UPPERCASE_SENTENCE_START")) {
            Object decalage = match.get("offset");
            if (decalage instanceof Number nombre && nombre.intValue() == debutDePage) return true;
        }
        return false;
    }

    /**
     * Vérifie que la suggestion ne modifie qu'un seul mot : même nombre de mots,
     * et un seul qui diffère.
     */
    private boolean unSeulMotChange(String motFautif, String suggestion) {
        String[] avant = motFautif.trim().split("\\s+");
        String[] apres = suggestion.trim().split("\\s+");
        if (avant.length != apres.length) return false;

        int differences = 0;
        for (int i = 0; i < avant.length; i++) {
            if (!avant[i].equals(apres[i])) differences++;
        }
        return differences <= 1;
    }

    /**
     * Extrait les mots des suggestions qui découpent le mot fautif, en écartant
     * les fragments trop courts pour être un mot recherché.
     */
    private List<String> fragments(List<String> suggestions) {
        List<String> mots = new ArrayList<>();
        for (String suggestion : suggestions) {
            for (String fragment : suggestion.split("\\s+")) {
                if (fragment.length() >= 4 && !mots.contains(fragment)) mots.add(fragment);
            }
        }
        return mots;
    }

    private int compterEspaces(String texte) {
        return (int) texte.chars().filter(Character::isSpaceChar).count();
    }

    /** Distance de Levenshtein (nombre minimal d'ajouts, suppressions ou remplacements). */
    private int distance(String a, String b) {
        int[] precedente = new int[b.length() + 1];
        int[] courante = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) precedente[j] = j;

        for (int i = 1; i <= a.length(); i++) {
            courante[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cout = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                courante[j] = Math.min(Math.min(courante[j - 1] + 1, precedente[j] + 1), precedente[j - 1] + cout);
            }
            int[] echange = precedente;
            precedente = courante;
            courante = echange;
        }
        return precedente[b.length()];
    }

    /** Découpe sur des fins de phrase pour ne pas couper un mot en deux. */
    private List<String> decouper(String texte) {
        List<String> morceaux = new ArrayList<>();
        if (texte.length() <= TAILLE_MAX_REQUETE) {
            morceaux.add(texte);
            return morceaux;
        }

        int debut = 0;
        while (debut < texte.length()) {
            int fin = Math.min(debut + TAILLE_MAX_REQUETE, texte.length());
            if (fin < texte.length()) {
                int coupure = texte.lastIndexOf(' ', fin);
                if (coupure > debut) fin = coupure;
            }
            morceaux.add(texte.substring(debut, fin));
            debut = fin;
        }
        return morceaux;
    }
}
