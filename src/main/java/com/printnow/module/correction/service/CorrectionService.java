package com.printnow.module.correction.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.printnow.module.correction.dto.FauteDTO;
import com.printnow.module.correction.model.VerificationOrthographe;
import com.printnow.module.correction.repository.VerificationOrthographeRepository;
import com.printnow.module.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Vérification orthographique payante d'un PDF avant impression.
 *
 * Le déroulé est volontairement en deux temps :
 *  1. l'analyse est gratuite et ne renvoie que le nombre de fautes ;
 *  2. le détail des fautes et le PDF corrigé ne sont délivrés qu'après paiement.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CorrectionService {

    // ─── Tarification ─────────────────────────────────────────────────────────
    /** Forfait couvrant les premières pages. */
    private static final BigDecimal PRIX_FORFAIT = new BigDecimal("2.90");
    /** Nombre de pages incluses dans le forfait. */
    private static final int PAGES_INCLUSES = 10;
    /** Prix de chaque page au-delà du forfait. */
    private static final BigDecimal PRIX_PAGE_SUPP = new BigDecimal("0.20");

    /** Nombre de corrections essayées en contexte avant d'abandonner. */
    private static final int MAX_CANDIDATS = 6;

    private final VerificationOrthographeRepository repository;
    private final LanguageToolClient languageTool;
    private final CorrecteurIaService correcteurIa;
    private final CorrecteurPdfService correcteurPdf;
    private final QuotaAnalyse quota;
    private final ProgressionAnalyse progression;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    /**
     * Plafond de pages par analyse.
     *
     * L'analyse est gratuite et sollicite un service tiers dont le quota est
     * limité : sans plafond, un seul document suffirait à l'épuiser. La valeur
     * est large — elle couvre un mémoire — mais borne le travail engagé avant
     * tout paiement.
     */
    @Value("${correction.pages.max:100}")
    private int pagesMax;

    /**
     * 2,90 € pour les 10 premières pages, puis 0,20 € par page supplémentaire.
     */
    public static BigDecimal calculerPrix(int nbPages) {
        if (nbPages <= PAGES_INCLUSES) return PRIX_FORFAIT;
        return PRIX_FORFAIT.add(PRIX_PAGE_SUPP.multiply(BigDecimal.valueOf(nbPages - PAGES_INCLUSES)));
    }

    /**
     * Analyse gratuite : stocke le PDF, compte les fautes et calcule le prix.
     * Le détail des fautes n'est pas retourné à ce stade.
     */
    @Transactional
    public VerificationOrthographe analyser(MultipartFile fichier, User client, String suivi) {
        if (fichier == null || fichier.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucun fichier fourni.");
        }
        if (!languageTool.estDisponible()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Le service de correction est momentanément indisponible.");
        }

        progression.publier(suivi, 5, "Lecture du document");
        Path chemin = stocker(fichier, client);

        try (PDDocument document = Loader.loadPDF(chemin.toFile())) {
            int nbPages = document.getNumberOfPages();
            if (nbPages > pagesMax) {
                supprimerSilencieusement(chemin);
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "La vérification est limitée à " + pagesMax + " pages ; ce document en compte " + nbPages + ".");
            }

            List<String> textes = ExtracteurTextePdf.extraireParPage(document);

            boolean contientDuTexte = textes.stream().anyMatch(t -> t != null && !t.isBlank());
            if (!contientDuTexte) {
                supprimerSilencieusement(chemin);
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Ce PDF ne contient pas de texte analysable (document scanné ou composé d'images).");
            }

            // Le quota n'est décompté qu'ici : un document refusé plus haut n'a
            // rien coûté, il serait injuste de le retenir contre le client.
            if (!quota.autoriser(client.getId())) {
                supprimerSilencieusement(chemin);
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Vous avez lancé trop d'analyses coup sur coup. Patientez une heure avant de réessayer.");
            }

            String texteComplet = String.join("\n", textes);

            // Le modèle n'a besoin que du texte brut, disponible dès maintenant :
            // on le consulte pendant que LanguageTool travaille au lieu de
            // l'attendre à la fin. Les deux lectures sont indépendantes jusqu'à
            // leur fusion.
            CompletableFuture<Map<String, String>> relecture =
                    CompletableFuture.supplyAsync(() -> correcteurIa.proposer(texteComplet));

            // Première lecture, toutes pages groupées en un minimum de requêtes.
            progression.publier(suivi, 10, "Recherche des fautes");
            List<FauteDTO> fautes = new ArrayList<>(languageTool.analyserPages(textes));

            // La validation s'appuie sur le texte complet : une phrase peut
            // enjamber deux pages, et la juger sur une seule priverait le
            // correcteur des mots dont il a besoin (« nous avons » en fin de
            // page, « netoyer » au début de la suivante).
            progression.publier(suivi, 45, "Vérification de chaque correction");
            validerEnContexte(fautes, texteComplet);

            // Seconde lecture, sur le texte une fois les corrections validées
            // appliquées. Elle doit impérativement venir après la validation :
            // menée sur des corrections encore approximatives, elle signalerait
            // des fautes provoquées par ces approximations.
            progression.publier(suivi, 62, "Seconde lecture");
            fautes.addAll(secondePasse(fautes, textes, texteComplet));

            // Contre-épreuve : certains signalements ne tiennent que tant que
            // leurs voisins sont fautifs. Elle précède la relecture du modèle,
            // dont les propositions portent justement sur ce que les règles ne
            // savent pas juger et ne survivraient pas à cette épreuve.
            progression.publier(suivi, 75, "Contre-épreuve des signalements");
            confirmerEnContexte(fautes, texteComplet);

            // Relecture par le modèle de langue, qui repère les fautes portant
            // sur des mots corrects et tranche les accords que les règles ne
            // savent pas résoudre. Ses propositions sont filtrées ci-dessous.
            progression.publier(suivi, 82, "Relecture approfondie");
            fusionnerRelectureIa(fautes, textes, texteComplet, attendre(relecture));

            // Toutes les sources ont contribué : on peut enfin savoir quels mots
            // sont mal orthographiés, et écarter les accords bâtis sur eux.
            progression.publier(suivi, 95, "Finalisation");
            fautes = ecarterAccordsTrompeurs(fautes);

            VerificationOrthographe verification = VerificationOrthographe.builder()
                    .client(client)
                    .nomFichier(fichier.getOriginalFilename())
                    .cheminOriginal(chemin.toString())
                    .nbPages(nbPages)
                    .nbFautes(fautes.size())
                    .prix(calculerPrix(nbPages))
                    .payee(false)
                    .resultatAnalyse(serialiser(fautes))
                    .dateCreation(LocalDateTime.now())
                    .build();

            return repository.save(verification);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (CorrecteurIaService.RelectureIndisponible e) {
            // Mieux vaut ne rien proposer qu'une correction dégradée : sans le
            // modèle, l'analyse laisse passer des fautes et en introduit de
            // fausses. On refuse donc comme lorsque LanguageTool est injoignable.
            supprimerSilencieusement(chemin);
            log.error("Relecture par le modèle impossible : analyse refusée", e);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Le service de correction est momentanément indisponible. Réessayez dans quelques instants.");
        } catch (IOException e) {
            supprimerSilencieusement(chemin);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fichier PDF illisible.");
        } finally {
            // Succès comme échec, le suivi n'a plus lieu d'être : le navigateur
            // a reçu sa réponse et cesse d'interroger.
            progression.terminer(suivi);
        }
    }

    /**
     * Génère le PDF corrigé, une fois la commande payée.
     *
     * @param indicesIgnores positions, dans la liste des fautes, que le client a
     *                       choisi de ne pas corriger (nom propre, terme métier…)
     */
    @Transactional
    public List<FauteDTO> appliquer(VerificationOrthographe verification,
                                    List<Integer> indicesIgnores,
                                    Map<Integer, String> remplacementsChoisis) {
        List<FauteDTO> toutesLesFautes = deserialiser(verification.getResultatAnalyse());
        List<FauteDTO> aCorriger = retenir(toutesLesFautes, indicesIgnores, remplacementsChoisis);

        Path source = Paths.get(verification.getCheminOriginal());
        Path destination = source.resolveSibling(
                source.getFileName().toString().replaceFirst("\\.pdf$", "") + "-corrige.pdf");

        try (PDDocument document = Loader.loadPDF(source.toFile())) {
            CorrecteurPdfService.Resultat resultat = correcteurPdf.corriger(document, aCorriger);
            document.save(destination.toFile());

            verification.setCheminCorrige(destination.toString());
            verification.setNbCorrigees(resultat.nbCorrigees());
            verification.setPayee(true);
            verification.setDatePaiement(LocalDateTime.now());
            // On réenregistre la liste complète : les fautes traitées portent
            // désormais leur statut, les ignorées restent à « non corrigée ».
            verification.setResultatAnalyse(serialiser(toutesLesFautes));
            repository.save(verification);

            return toutesLesFautes;
        } catch (IOException e) {
            log.error("Génération du PDF corrigé impossible (vérification {})", verification.getId(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Impossible de générer le PDF corrigé.");
        }
    }

    public List<FauteDTO> fautesDe(VerificationOrthographe verification) {
        return deserialiser(verification.getResultatAnalyse());
    }

    /**
     * Applique les choix du client à la liste des fautes : écarte celles qu'il a
     * refusées et retient la suggestion qu'il a préférée.
     *
     * Utilisé aussi bien pour l'aperçu que pour le PDF final, afin que ce qu'il
     * voit corresponde exactement à ce qu'il recevra.
     */
    public List<FauteDTO> retenir(List<FauteDTO> fautes,
                                  List<Integer> indicesIgnores,
                                  Map<Integer, String> remplacementsChoisis) {
        List<Integer> ignores = indicesIgnores == null ? List.of() : indicesIgnores;
        Map<Integer, String> choix = remplacementsChoisis == null ? Map.of() : remplacementsChoisis;

        List<FauteDTO> retenues = new ArrayList<>();
        for (int i = 0; i < fautes.size(); i++) {
            if (ignores.contains(i)) continue;

            FauteDTO faute = fautes.get(i);
            String choisi = choix.get(i);
            // On n'accepte qu'un mot figurant parmi les suggestions : le client
            // ne peut pas faire réécrire un texte arbitraire dans son PDF.
            if (choisi != null && faute.getSuggestions() != null && faute.getSuggestions().contains(choisi)) {
                faute.setCorrection(choisi);
            }
            retenues.add(faute);
        }
        return retenues;
    }

    /**
     * Seconde lecture du document, une fois les corrections validées appliquées.
     *
     * Lorsque deux fautes sont proches, LanguageTool ne signale que l'une des
     * deux : ses règles portent parfois sur un groupe de mots, et il écarte les
     * signalements qui se chevauchent. Relire le texte corrigé fait apparaître
     * celles qui étaient masquées.
     *
     * @return les fautes nouvellement découvertes, à traiter après les premières
     */
    private List<FauteDTO> secondePasse(List<FauteDTO> premieres, List<String> textes, String texteComplet) {
        List<FauteDTO> nouvelles = new ArrayList<>();
        Set<String> dejaSignalees = premieres.stream().map(FauteDTO::getMotFautif).collect(Collectors.toSet());

        // Les pages inchangées sont laissées vides : l'analyse groupée les ignore,
        // tout en conservant la numérotation d'origine.
        List<String> corriges = new ArrayList<>();
        for (String texte : textes) {
            String corrige = appliquerEnMemoire(texte, premieres);
            corriges.add(corrige.equals(texte) ? "" : corrige);
        }

        for (FauteDTO faute : languageTool.analyserPages(corriges)) {
            // On ne retient que les fautes présentes dans le document d'origine :
            // les autres portent sur du texte issu de la première passe, et les
            // corriger reviendrait à corriger une correction.
            String origine = textes.get(faute.getPage() - 1);
            if (!presentDansLeTexte(origine, faute.getMotFautif())) continue;
            if (!dejaSignalees.add(faute.getMotFautif())) continue;
            nouvelles.add(faute);
        }

        validerEnContexte(nouvelles, texteComplet);
        return nouvelles;
    }

    /**
     * Écarte les signalements qui ne subsistent pas une fois les autres fautes
     * de la phrase corrigées.
     *
     * Les règles d'accord raisonnent sur les mots voisins ; si l'un d'eux est
     * mal orthographié, elles ne le reconnaissent pas et incriminent le mot
     * correct d'à côté. « la connection internet était presque inexistante »
     * fait signaler « inexistante », que le correcteur voudrait mettre au
     * masculin faute de reconnaître « connection ». Une fois « connexion »
     * rétabli, le signalement disparaît de lui-même.
     *
     * Les fautes seules dans leur phrase ne sont pas réexaminées : rien
     * n'aurait changé autour d'elles.
     */
    private void confirmerEnContexte(List<FauteDTO> fautes, String texteComplet) {
        List<FauteDTO> aReexaminer = new ArrayList<>();
        List<LanguageToolClient.Epreuve> epreuves = new ArrayList<>();

        for (FauteDTO faute : fautes) {
            String phrase = phraseDeLaFaute(texteComplet, faute);
            if (phrase == null) continue; // dans le doute, on conserve le signalement

            String assainie = appliquerEnMemoire(phrase, fautes.stream().filter(f -> f != faute).toList());
            if (assainie.equals(phrase)) continue;

            aReexaminer.add(faute);
            epreuves.add(new LanguageToolClient.Epreuve(assainie, faute.getMotFautif()));
        }
        if (aReexaminer.isEmpty()) return;

        // On éprouve avec les mêmes filtres que l'analyse : le contrôle
        // d'orthographe pure ne verrait pas les règles d'accord, qui sont
        // précisément celles dont le verdict est ici remis en question.
        List<LanguageToolClient.Verdict> verdicts = languageTool.eprouverEnLot(epreuves);

        List<FauteDTO> infirmees = new ArrayList<>();
        for (int i = 0; i < aReexaminer.size(); i++) {
            if (verdicts.get(i).accepte()) infirmees.add(aReexaminer.get(i));
        }
        fautes.removeAll(infirmees);
    }

    /**
     * Retient, pour chaque faute, la correction que LanguageTool accepte une fois
     * replacée dans la phrase.
     *
     * Une suggestion bien orthographiée peut rester fautive en contexte :
     * « nettoyer » est un mot correct, mais « nous avons nettoyer » ne l'est pas.
     * On essaie donc les candidats dans l'ordre jusqu'à en trouver un que le
     * correcteur ne signale plus.
     */
    private void validerEnContexte(List<FauteDTO> fautes, String textePage) {
        List<Epreuve> encours = new ArrayList<>();
        for (FauteDTO faute : fautes) {
            String phrase = phraseDeLaFaute(textePage, faute);
            if (phrase == null) continue;

            List<String> candidats = candidats(faute);
            if (candidats.isEmpty()) continue;

            encours.add(new Epreuve(faute, phrase, candidats));
        }

        // Les candidats sont éprouvés par tours : à chaque tour, toutes les
        // fautes encore indécises soumettent leur candidat suivant, et le tout
        // part en une seule requête. La plupart sont réglées au premier tour.
        for (int tour = 0; tour < MAX_CANDIDATS * 2 && !encours.isEmpty(); tour++) {
            List<Epreuve> aTester = new ArrayList<>();
            List<LanguageToolClient.Epreuve> requetes = new ArrayList<>();

            for (Epreuve epreuve : encours) {
                String candidat = epreuve.suivant();
                if (candidat == null) continue; // candidats épuisés : la faute reste en l'état
                aTester.add(epreuve);
                requetes.add(new LanguageToolClient.Epreuve(
                        remplacerMot(epreuve.phrase(), epreuve.faute().getMotFautif(), candidat),
                        dernierMot(candidat)));
            }
            if (aTester.isEmpty()) break;

            List<LanguageToolClient.Verdict> verdicts = languageTool.eprouverEnLot(requetes);

            List<Epreuve> indecises = new ArrayList<>();
            for (int i = 0; i < aTester.size(); i++) {
                Epreuve epreuve = aTester.get(i);
                if (verdicts.get(i).accepte()) {
                    retenirCorrection(epreuve.faute(), epreuve.candidatCourant());
                    continue;
                }
                epreuve.noterPiste(verdicts.get(i).aEssayer());
                indecises.add(epreuve);
            }
            encours = indecises;
        }
    }

    /**
     * Épreuve en cours pour une faute : la phrase où la juger, et les candidats
     * qu'il reste à essayer.
     *
     * En cas de rejet, le correcteur propose souvent lui-même une autre forme :
     * elle passe en tête, pour être essayée au tour suivant. Comme dans la
     * version séquentielle, une piste ne peut en engendrer une autre — sans quoi
     * l'épreuve pourrait s'enchaîner sans fin.
     */
    private static final class Epreuve {
        private final FauteDTO faute;
        private final String phrase;
        private final Deque<String> restants;
        private String candidatCourant;
        private boolean courantEstUnePiste;
        private boolean prochainEstUnePiste;

        Epreuve(FauteDTO faute, String phrase, List<String> candidats) {
            this.faute = faute;
            this.phrase = phrase;
            this.restants = new ArrayDeque<>(candidats);
        }

        FauteDTO faute() { return faute; }
        String phrase() { return phrase; }
        String candidatCourant() { return candidatCourant; }

        /** Candidat suivant à éprouver, ou null s'il n'en reste plus. */
        String suivant() {
            candidatCourant = restants.poll();
            courantEstUnePiste = prochainEstUnePiste;
            prochainEstUnePiste = false;
            return candidatCourant;
        }

        /** Retient la forme proposée par le correcteur, à essayer au tour suivant. */
        void noterPiste(String piste) {
            if (courantEstUnePiste) return; // une piste n'en engendre pas une autre
            if (piste == null || piste.equals(candidatCourant)) return;
            restants.addFirst(piste);
            prochainEstUnePiste = true;
        }
    }

    /**
     * Récupère le résultat de la relecture menée en parallèle.
     *
     * Une tâche asynchrone enveloppe ses erreurs : on redonne à l'indisponibilité
     * du modèle sa forme d'origine, pour qu'elle soit traitée comme si l'appel
     * avait été fait sur place.
     */
    private Map<String, String> attendre(CompletableFuture<Map<String, String>> relecture) {
        try {
            return relecture.join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof CorrecteurIaService.RelectureIndisponible indisponible) {
                throw indisponible;
            }
            throw e;
        }
    }

    /** Retient la correction validée et la place en tête des suggestions. */
    private void retenirCorrection(FauteDTO faute, String retenu) {
        faute.setCorrection(retenu);
        List<String> suggestions = new ArrayList<>(
                faute.getSuggestions() == null ? List.of() : faute.getSuggestions());
        suggestions.remove(retenu);
        suggestions.add(0, retenu);
        faute.setSuggestions(suggestions);
    }

    /**
     * Intègre la relecture du modèle de langue à l'analyse existante.
     *
     * Le modèle prime sur LanguageTool pour le choix de la forme : les mesures
     * montrent qu'il tranche mieux les accords. En revanche il n'ajoute une
     * faute que si le mot visé figure réellement dans le document et que la
     * correction est un mot correctement orthographié — ce dernier point étant
     * vérifié par LanguageTool, qui l'empêche d'inventer.
     */
    private void fusionnerRelectureIa(List<FauteDTO> fautes, List<String> textes, String texteComplet,
                                      Map<String, String> propositions) {
        if (propositions.isEmpty()) return;

        Map<String, FauteDTO> parMot = new HashMap<>();
        for (FauteDTO faute : fautes) parMot.putIfAbsent(faute.getMotFautif(), faute);

        // Les contrôles bon marché d'abord ; les propositions qui les passent
        // sont ensuite éprouvées toutes ensemble, en une requête au lieu d'une
        // par proposition.
        Map<String, String> aEprouver = new LinkedHashMap<>();
        for (Map.Entry<String, String> proposition : propositions.entrySet()) {
            String mot = proposition.getKey();
            String correction = proposition.getValue();

            if (!presentDansLeTexte(texteComplet, mot)) continue;

            FauteDTO existante = parMot.get(mot);
            if (existante != null) {
                if (correction.equals(existante.getCorrection())) continue;
            } else {
                // Faute inconnue de LanguageTool : le mot est donc bien
                // orthographié, et seul un accord peut être en cause. On
                // n'accepte alors qu'une variation de terminaison : « prudent »
                // → « prudents » est plausible, « donnaient » → « donne » relève
                // de la réécriture.
                if (Math.abs(mot.length() - correction.length()) > 2) continue;
                if (!memeRacine(mot, correction)) continue;
            }
            aEprouver.put(mot, correction);
        }
        if (aEprouver.isEmpty()) return;

        Set<String> retenues = eprouverPropositions(texteComplet, aEprouver);

        for (Map.Entry<String, String> proposition : aEprouver.entrySet()) {
            String mot = proposition.getKey();
            String correction = proposition.getValue();
            if (!retenues.contains(mot)) continue;

            FauteDTO existante = parMot.get(mot);
            if (existante != null) {
                // Faute déjà repérée : on retient la forme choisie par le modèle,
                // en gardant les autres propositions à portée de clic.
                List<String> suggestions = new ArrayList<>(
                        existante.getSuggestions() == null ? List.of() : existante.getSuggestions());
                suggestions.remove(correction);
                suggestions.add(0, correction);
                existante.setSuggestions(suggestions);
                existante.setCorrection(correction);
                continue;
            }

            int page = pageContenant(textes, mot);
            String phrase = phraseContenant(texteComplet, mot);
            fautes.add(new FauteDTO(page, mot, correction, List.of(correction),
                    "Faute repérée à la relecture.", phrase == null ? "" : phrase, false));
        }
    }

    /**
     * Soumet toutes les propositions du modèle au contrôle en contexte, en une
     * seule requête.
     *
     * C'est ce qui empêche le modèle d'écraser une correction déjà vérifiée : il
     * propose parfois « nettoyer » là où la phrase appelle « nettoyé », et le
     * contrôle le refuse puisque « nous avons nettoyer » reste fautif.
     *
     * @return les mots dont la correction proposée tient dans sa phrase
     */
    private Set<String> eprouverPropositions(String texteComplet, Map<String, String> propositions) {
        List<String> mots = new ArrayList<>();
        List<LanguageToolClient.Epreuve> epreuves = new ArrayList<>();
        Set<String> retenues = new HashSet<>();

        for (Map.Entry<String, String> proposition : propositions.entrySet()) {
            String mot = proposition.getKey();
            String correction = proposition.getValue();
            String phrase = phraseContenant(texteComplet, mot);

            if (phrase == null) {
                // Sans phrase exploitable, on se contente de vérifier que le mot existe.
                if (!languageTool.estMalOrthographie(correction + ".", correction)) retenues.add(mot);
                continue;
            }
            mots.add(mot);
            epreuves.add(new LanguageToolClient.Epreuve(
                    remplacerMot(phrase, mot, correction), dernierMot(correction)));
        }

        List<LanguageToolClient.Verdict> verdicts = languageTool.eprouverEnLot(epreuves);
        for (int i = 0; i < mots.size(); i++) {
            if (verdicts.get(i).accepte()) retenues.add(mots.get(i));
        }
        return retenues;
    }

    /**
     * Écarte les signalements portant sur un groupe de mots.
     *
     * Ces règles d'accord ne se prononcent de façon fiable que sur des phrases
     * déjà correctes ; face à une faute, elles désignent presque toujours le
     * mauvais coupable. « une grande foret » devient « une grand foret » — le
     * correcteur a reconnu « foret », l'outil, masculin — et « regardé les
     * vidéos et discuté » devient « les vidéos et discutés », alors que le
     * participe se rapportait à « nous avons ». Dans chaque cas, la vraie faute
     * est signalée par ailleurs sur le mot isolé, qui suffit à réparer la phrase.
     *
     * S'y ajoute une raison technique : un groupe de mots est réparti sur
     * plusieurs objets texte du PDF, et ne peut donc pas être réécrit d'un seul
     * tenant sans refaire la mise en page.
     */
    private List<FauteDTO> ecarterAccordsTrompeurs(List<FauteDTO> fautes) {
        return fautes.stream()
                .filter(faute -> !faute.getMotFautif().contains(" "))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Les deux mots partagent-ils une racine suffisante pour n'être qu'une
     * variation d'accord ou de conjugaison ?
     */
    private boolean memeRacine(String mot, String correction) {
        String a = mot.toLowerCase();
        String b = correction.toLowerCase();
        int commun = 0;
        while (commun < a.length() && commun < b.length() && a.charAt(commun) == b.charAt(commun)) commun++;
        return commun >= Math.min(a.length(), b.length()) - 2;
    }

    /** Numéro de la page (1 = première) contenant le mot, 1 par défaut. */
    private int pageContenant(List<String> textes, String mot) {
        for (int i = 0; i < textes.size(); i++) {
            if (presentDansLeTexte(textes.get(i), mot)) return i + 1;
        }
        return 1;
    }


    /**
     * Candidats à essayer, dans l'ordre de préférence : les suggestions de
     * LanguageTool, puis les fragments des suggestions qui découpent le mot.
     * « clairesse » n'a ainsi que « claires se » comme piste, dont « claires »
     * est la correction réellement attendue.
     */
    private List<String> candidats(FauteDTO faute) {
        List<String> candidats = new ArrayList<>();
        if (faute.getSuggestions() != null) candidats.addAll(faute.getSuggestions());

        // Un mot fautif isolé ne peut être réparé que par un mot isolé.
        if (!faute.getMotFautif().trim().contains(" ")) {
            for (String suggestion : new ArrayList<>(candidats)) {
                if (!suggestion.contains(" ")) continue;
                for (String fragment : suggestion.split("\\s+")) {
                    if (fragment.length() > 2 && !candidats.contains(fragment)) candidats.add(fragment);
                }
            }
            candidats.removeIf(c -> c.contains(" "));
        }
        return candidats.stream().limit(MAX_CANDIDATS).toList();
    }

    /**
     * Phrase du document où juger la faute.
     *
     * Un même mot peut figurer plusieurs fois, correct ici et fautif là :
     * « nous sommes sortis visiter le centre-ville », puis « nous avons visiter
     * un château ». Se fier à la première occurrence venue conclurait que le mot
     * est bien employé, et la faute serait corrigée par elle-même. On retient
     * donc la phrase qui correspond à l'extrait rapporté par le correcteur.
     */
    private String phraseDeLaFaute(String texte, FauteDTO faute) {
        List<String> phrases = phrasesContenant(texte, faute.getMotFautif());
        if (phrases.isEmpty()) return null;
        if (phrases.size() == 1) return phrases.get(0);

        String extrait = faute.getContexte();
        if (extrait == null || extrait.isBlank()) return phrases.get(0);

        // La phrase qui partage le plus de mots avec l'extrait est la bonne.
        return phrases.stream()
                .max(Comparator.comparingLong(phrase -> motsCommuns(phrase, extrait)))
                .orElse(phrases.get(0));
    }

    /** Nombre de mots un peu significatifs que la phrase et l'extrait ont en commun. */
    private long motsCommuns(String phrase, String extrait) {
        Set<String> motsExtrait = new HashSet<>(List.of(extrait.toLowerCase().split("\\W+")));
        return List.of(phrase.toLowerCase().split("\\W+")).stream()
                .filter(mot -> mot.length() > 3)
                .distinct()
                .filter(motsExtrait::contains)
                .count();
    }

    /** Extrait la phrase du texte contenant le mot, pour valider dans son contexte réel. */
    private String phraseContenant(String texte, String mot) {
        List<String> phrases = phrasesContenant(texte, mot);
        return phrases.isEmpty() ? null : phrases.get(0);
    }

    /** Toutes les phrases du texte où le mot apparaît. */
    private List<String> phrasesContenant(String texte, String mot) {
        List<String> phrases = new ArrayList<>();
        Matcher position = Pattern.compile("\\b" + Pattern.quote(mot) + "\\b", Pattern.UNICODE_CHARACTER_CLASS)
                .matcher(texte);

        while (position.find()) {
            int debut = texte.lastIndexOf('.', position.start());
            int fin = texte.indexOf('.', position.end());
            debut = debut < 0 ? 0 : debut + 1;
            fin = fin < 0 ? texte.length() : fin + 1;

            String phrase = texte.substring(debut, fin).replaceAll("\\s+", " ").trim();
            if (!phrase.isBlank() && !phrases.contains(phrase)) phrases.add(phrase);
        }
        return phrases;
    }

    private String remplacerMot(String phrase, String mot, String remplacement) {
        return Pattern.compile("\\b" + Pattern.quote(mot) + "\\b", Pattern.UNICODE_CHARACTER_CLASS)
                .matcher(phrase)
                .replaceAll(Matcher.quoteReplacement(remplacement));
    }

    /** Sur une correction en plusieurs mots, seul le dernier porte généralement le changement. */
    private String dernierMot(String candidat) {
        String[] mots = candidat.trim().split("\\s+");
        return mots[mots.length - 1];
    }

    /** Le mot figure-t-il tel quel dans le texte, en tant que mot entier ? */
    private boolean presentDansLeTexte(String texte, String mot) {
        if (mot == null || mot.isBlank()) return false;
        return Pattern.compile("\\b" + Pattern.quote(mot) + "\\b", Pattern.UNICODE_CHARACTER_CLASS)
                .matcher(texte)
                .find();
    }

    /** Applique les corrections au texte, uniquement pour relancer l'analyse. */
    private String appliquerEnMemoire(String texte, List<FauteDTO> fautes) {
        String resultat = texte;
        for (FauteDTO faute : fautes) {
            if (faute.getCorrection() == null || faute.getCorrection().isBlank()) continue;
            resultat = Pattern.compile("\\b" + Pattern.quote(faute.getMotFautif()) + "\\b",
                            Pattern.UNICODE_CHARACTER_CLASS)
                    .matcher(resultat)
                    .replaceAll(Matcher.quoteReplacement(faute.getCorrection()));
        }
        return resultat;
    }

    // ─── Utilitaires ──────────────────────────────────────────────────────────

    private Path stocker(MultipartFile fichier, User client) {
        try {
            Path dossier = Paths.get(uploadDir, "corrections", String.valueOf(client.getId()));
            Files.createDirectories(dossier);
            Path cible = dossier.resolve(UUID.randomUUID() + ".pdf");
            Files.copy(fichier.getInputStream(), cible, StandardCopyOption.REPLACE_EXISTING);
            return cible;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Impossible d'enregistrer le fichier.");
        }
    }

    private void supprimerSilencieusement(Path chemin) {
        try {
            Files.deleteIfExists(chemin);
        } catch (IOException e) {
            log.debug("Suppression impossible : {}", chemin, e);
        }
    }

    private String serialiser(List<FauteDTO> fautes) {
        try {
            return objectMapper.writeValueAsString(fautes);
        } catch (Exception e) {
            log.warn("Sérialisation des fautes impossible", e);
            return "[]";
        }
    }

    private List<FauteDTO> deserialiser(String json) {
        try {
            if (json == null || json.isBlank()) return new ArrayList<>();
            return new ArrayList<>(List.of(objectMapper.readValue(json, FauteDTO[].class)));
        } catch (Exception e) {
            log.warn("Lecture des fautes impossible", e);
            return new ArrayList<>();
        }
    }
}
