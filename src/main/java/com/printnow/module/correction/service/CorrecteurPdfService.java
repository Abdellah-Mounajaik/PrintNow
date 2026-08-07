package com.printnow.module.correction.service;

import com.printnow.module.correction.dto.FauteDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdfwriter.ContentStreamWriter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationHighlight;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Produit le PDF corrigé.
 *
 * Une faute n'est réécrite dans le document que si l'opération est sûre :
 *  - le mot fautif doit être présent d'un seul tenant dans le flux de texte
 *    (les générateurs de PDF découpent parfois les mots pour le crénage) ;
 *  - la police intégrée doit contenir les caractères de la correction (les
 *    polices sont généralement « sous-ensemblées » : un « ê » jamais utilisé
 *    dans le document est absent du fichier) ;
 *  - la correction ne doit pas être sensiblement plus large que le mot d'origine,
 *    sans quoi elle chevaucherait le texte suivant.
 *
 * Toute faute ne remplissant pas ces conditions est simplement surlignée, avec
 * la correction en commentaire : la mise en page reste ainsi toujours intacte.
 */
@Service
@Slf4j
public class CorrecteurPdfService {

    /**
     * Bornes de l'ajustement horizontal appliqué au texte corrigé.
     *
     * Les traitements de texte positionnent chaque passage en absolu (« Tm ») :
     * un mot réécrit plus large chevauche donc le suivant, un mot plus court
     * laisse un blanc, sans que rien ne se réajuste. On compense en jouant sur
     * l'échelle horizontale, afin que la correction occupe exactement la place
     * du mot d'origine.
     *
     * Les bornes sont larges à dessein : une correction ajoutant trois lettres
     * à un mot court (« magnifik » → « magnifiques ») demande près de 30 % de
     * compression. Une lettre légèrement condensée reste discrète, là où deux
     * mots soudés rendraient la phrase illisible. Au-delà, on renonce à
     * réécrire et la faute est simplement annotée.
     */
    private static final float AJUSTEMENT_MIN = 0.70f;
    private static final float AJUSTEMENT_MAX = 1.40f;

    /** Résultat de la génération : le document et le nombre de fautes réellement réécrites. */
    public record Resultat(int nbCorrigees, List<FauteDTO> fautes) {}

    /** Police active dans le flux : son nom de ressource, son corps et sa fonte. */
    private record Police(COSName nom, float taille, PDFont fonte) {}

    /**
     * Désigne l'occurrence à réécrire lorsqu'un mot fautif figure plusieurs fois
     * dans la page.
     *
     * « nous sommes sortis visiter le centre-ville » puis « nous avons visiter un
     * château » : seule la seconde est fautive. Sans ce repère, la réécriture
     * abîmait la première. Un rang absent vise toutes les occurrences, comme
     * auparavant.
     */
    private static final class Cible {
        private final Integer rang;
        private int rencontrees;

        Cible(Integer rang) {
            this.rang = rang;
        }

        /** Faut-il réécrire l'occurrence que l'on vient de rencontrer ? */
        boolean retenir() {
            rencontrees++;
            return rang == null || rencontrees == rang;
        }

        /** L'occurrence visée est passée : inutile de poursuivre la recherche. */
        boolean depassee() {
            return rang != null && rencontrees >= rang;
        }
    }

    /**
     * Applique les corrections au document. Les objets {@link FauteDTO} passés en
     * paramètre sont mis à jour pour indiquer lesquelles ont été réécrites.
     */
    public Resultat corriger(PDDocument document, List<FauteDTO> fautes) throws IOException {
        int nbCorrigees = 0;

        for (int numeroPage = 1; numeroPage <= document.getNumberOfPages(); numeroPage++) {
            final int page1Based = numeroPage;
            PDPage page = document.getPage(numeroPage - 1);
            List<FauteDTO> fautesPage = fautes.stream()
                    .filter(f -> f.getPage() != null && f.getPage() == page1Based)
                    .toList();
            if (fautesPage.isEmpty()) continue;

            List<FauteDTO> nonCorrigees = new ArrayList<>();
            for (FauteDTO faute : fautesPage) {
                boolean remplacee = faute.getCorrection() != null
                        && !faute.getCorrection().isBlank()
                        && (remplacerDansPage(document, page, faute.getMotFautif(), faute.getCorrection(),
                                              new Cible(faute.getOccurrence()))
                            || remplacerACheval(document, page, faute.getMotFautif(), faute.getCorrection(),
                                                new Cible(faute.getOccurrence())));

                faute.setCorrigeeDansPdf(remplacee);
                if (remplacee) {
                    nbCorrigees++;
                } else {
                    nonCorrigees.add(faute);
                }
            }

            if (!nonCorrigees.isEmpty()) {
                surligner(document, page, numeroPage, nonCorrigees);
            }
        }

        return new Resultat(nbCorrigees, fautes);
    }

    // ─── Réécriture du texte ──────────────────────────────────────────────────

    /**
     * Cherche le mot dans le flux de contenu de la page et le remplace si toutes
     * les conditions de sûreté sont réunies.
     *
     * @return true si le mot a effectivement été réécrit
     */
    private boolean remplacerDansPage(PDDocument document, PDPage page, String motFautif,
                                      String correction, Cible cible) {
        try {
            PDFStreamParser parser = new PDFStreamParser(page);
            List<Object> jetons = parser.parse();

            List<Object> resultat = new ArrayList<>();
            Police policeCourante = null;
            float echelleCourante = 100f; // valeur par défaut de l'opérateur Tz
            boolean modifie = false;

            for (int i = 0; i < jetons.size(); i++) {
                Object jeton = jetons.get(i);
                resultat.add(jeton);

                if (!(jeton instanceof Operator operateur)) continue;

                switch (operateur.getName()) {
                    case "Tf" -> policeCourante = resoudrePolice(page, jetons, i);
                    case "Tz" -> {
                        if (i > 0 && jetons.get(i - 1) instanceof COSNumber valeur) {
                            echelleCourante = valeur.floatValue();
                        }
                    }
                    case "Tj", "'" -> {
                        if (i > 0 && jetons.get(i - 1) instanceof COSString chaine
                                && appliquer(page, resultat, policeCourante, chaine,
                                             motFautif, correction, echelleCourante, cible)) {
                            modifie = true;
                        }
                    }
                    case "TJ" -> {
                        if (i > 0 && jetons.get(i - 1) instanceof COSArray tableau
                                && remplacerDansTableau(page, tableau, policeCourante, motFautif, cible,
                                                        correction, resultat, echelleCourante)) {
                            modifie = true;
                        }
                    }
                    default -> { /* opérateur sans incidence sur le texte */ }
                }
            }

            if (modifie) {
                reecrireContenu(document, page, resultat);
            }
            return modifie;

        } catch (Exception e) {
            log.debug("Remplacement impossible pour « {} »", motFautif, e);
            return false;
        }
    }

    // ─── Mots répartis sur plusieurs passages ─────────────────────────────────

    /** Un passage de texte du flux : son opérande, la police et l'échelle en vigueur. */
    private record Passage(int indexOperateur, COSBase operande, Police police, float echelle) {}

    /**
     * Seconde tentative, engagée seulement lorsque le mot n'a été trouvé dans
     * aucun passage pris isolément.
     *
     * Les traitements de texte scindent parfois un mot entre deux objets texte
     * distincts, chacun positionné en absolu : « captivante » est écrit « cap »
     * puis, ailleurs dans le flux, « tivante ». Le mot n'existe alors dans aucun
     * passage, et la première passe ne peut rien voir.
     *
     * On cherche donc une suite de passages consécutifs dont les textes, mis
     * bout à bout, forment exactement le mot fautif. La correction est écrite
     * dans le premier, les suivants sont vidés : les passages sont contigus —
     * l'extraction de texte les a d'ailleurs lus comme un seul mot —, si bien
     * que le résultat occupe la même place, à l'ajustement d'échelle près.
     */
    private boolean remplacerACheval(PDDocument document, PDPage page, String motFautif,
                                     String correction, Cible cible) {
        try {
            PDFStreamParser parser = new PDFStreamParser(page);
            List<Object> jetons = parser.parse();
            List<Passage> passages = repererPassages(page, jetons);

            for (int debut = 0; debut < passages.size(); debut++) {
                Police police = passages.get(debut).police();
                StringBuilder assemble = new StringBuilder();

                for (int fin = debut; fin < passages.size(); fin++) {
                    Passage passage = passages.get(fin);
                    // Deux polices différentes ne partagent pas le même encodage :
                    // les rapprocher n'aurait pas de sens.
                    if (!passage.police().nom().equals(police.nom())) break;

                    String texte = texteDuPassage(passage, police.fonte());
                    if (texte == null) break;
                    assemble.append(texte);

                    if (assemble.length() > motFautif.length()) break;
                    // Un passage isolé relève de la première passe.
                    if (fin > debut && assemble.toString().equals(motFautif)) {
                        // Les occurrences précédant celle qui est visée sont
                        // comptées puis laissées telles quelles.
                        if (!cible.retenir()) break;
                        if (ecrireACheval(document, page, jetons, passages, debut, fin, motFautif, correction)) {
                            return true;
                        }
                    }
                }
            }
            return false;

        } catch (Exception e) {
            log.debug("Remplacement à cheval impossible pour « {} »", motFautif, e);
            return false;
        }
    }

    /** Relève les passages de texte du flux, avec la police et l'échelle actives. */
    private List<Passage> repererPassages(PDPage page, List<Object> jetons) {
        List<Passage> passages = new ArrayList<>();
        Police police = null;
        float echelle = 100f;

        for (int i = 0; i < jetons.size(); i++) {
            if (!(jetons.get(i) instanceof Operator operateur)) continue;

            switch (operateur.getName()) {
                case "Tf" -> police = resoudrePolice(page, jetons, i);
                case "Tz" -> {
                    if (i > 0 && jetons.get(i - 1) instanceof COSNumber valeur) echelle = valeur.floatValue();
                }
                case "Tj", "'", "TJ" -> {
                    if (i > 0 && police != null && jetons.get(i - 1) instanceof COSBase operande) {
                        passages.add(new Passage(i, operande, police, echelle));
                    }
                }
                default -> { /* opérateur sans incidence sur le texte */ }
            }
        }
        return passages;
    }

    /** Texte affiché par un passage, ou null s'il est indéchiffrable. */
    private String texteDuPassage(Passage passage, PDFont police) {
        if (passage.operande() instanceof COSString chaine) return decoder(chaine, police);
        if (passage.operande() instanceof COSArray tableau) return texteDuTableau(tableau, police);
        return null;
    }

    /**
     * Écrit la correction dans le premier passage de la suite et vide les
     * autres, en compensant l'écart de largeur.
     */
    private boolean ecrireACheval(PDDocument document, PDPage page, List<Object> jetons, List<Passage> passages,
                                  int debut, int fin, String motFautif, String correction) throws IOException {
        Police police = passages.get(debut).police();
        float ajustement;
        try {
            ajustement = ajustement(police.fonte(), motFautif, police.fonte(), correction);
        } catch (IllegalArgumentException e) {
            return false; // correction non représentable dans cette police
        }
        if (ajustement == 0) return false;

        ecrireDansPassage(passages.get(debut), police.fonte(), correction);
        for (int position = debut + 1; position <= fin; position++) {
            ecrireDansPassage(passages.get(position), police.fonte(), "");
        }

        List<Object> resultat = new ArrayList<>(jetons);
        if (ajustement != 1f) {
            int indexOperateur = passages.get(debut).indexOperateur();
            float echelle = passages.get(debut).echelle();
            // On insère d'abord après l'opérateur : les indices antérieurs ne bougent pas.
            resultat.add(indexOperateur + 1, Operator.getOperator("Tz"));
            resultat.add(indexOperateur + 1, new COSFloat(echelle));
            resultat.add(indexOperateur - 1, Operator.getOperator("Tz"));
            resultat.add(indexOperateur - 1, new COSFloat(echelle * ajustement));
        }
        reecrireContenu(document, page, resultat);
        return true;
    }

    /** Fait porter tout le texte du passage à son premier fragment. */
    private void ecrireDansPassage(Passage passage, PDFont police, String texte) throws IOException {
        if (passage.operande() instanceof COSString chaine) {
            chaine.setValue(police.encode(texte));
            return;
        }
        if (!(passage.operande() instanceof COSArray tableau)) return;

        boolean premier = true;
        for (int position = 0; position < tableau.size(); position++) {
            if (tableau.get(position) instanceof COSString) {
                ecrire(tableau, position, police, premier ? texte : "");
                premier = false;
            } else if (tableau.get(position) instanceof COSNumber) {
                // Le crénage interne n'a plus lieu d'être : le texte est d'un seul tenant.
                tableau.set(position, new COSFloat(0));
            }
        }
    }

    // ─── Réécriture d'un passage isolé ────────────────────────────────────────

    /**
     * Réécrit une chaîne affichée par « Tj ».
     *
     * @param resultat flux en construction ; l'opérande déjà copié y est remplacé
     * @return true si la réécriture a pu être faite
     */
    private boolean appliquer(PDPage page, List<Object> resultat, Police police, COSString chaine,
                              String motFautif, String correction, float echelleCourante, Cible cible) {
        if (police == null) return false;

        String texteOrigine = decoder(chaine, police.fonte());
        if (texteOrigine == null || !texteOrigine.contains(motFautif)) return false;

        // Remplacement borné aux limites de mot : sans cela, corriger « sa » en
        // « ça » abîmerait aussi le « sa » contenu dans « intéressant ». Et seule
        // l'occurrence visée est réécrite, le mot pouvant être correct ailleurs.
        List<Integer> aReecrire = new ArrayList<>();
        Matcher chercheur = motifDeMot(motFautif).matcher(texteOrigine);
        for (int rang = 0; chercheur.find(); rang++) {
            if (cible.retenir()) aReecrire.add(rang);
        }
        if (aReecrire.isEmpty()) return false;
        String texteCorrige = reecrire(texteOrigine, motFautif, correction, aReecrire);

        try {
            Police ecriture = policeCapable(page, police, texteCorrige,
                                            texteOrigine.trim().equals(motFautif));
            if (ecriture == null) return false;

            float ajustement = ajustement(police.fonte(), texteOrigine, ecriture.fonte(), texteCorrige);
            if (ajustement == 0) return false;

            chaine.setValue(ecriture.fonte().encode(texteCorrige));
            encadrer(resultat, police, ecriture, echelleCourante, ajustement);
            return true;

        } catch (IllegalArgumentException | IOException e) {
            // Caractère non représentable dans la police intégrée
            return false;
        }
    }

    /**
     * Facteur d'échelle horizontale ramenant le texte corrigé à la largeur de
     * l'original.
     *
     * @return 1 si aucun ajustement n'est nécessaire ou mesurable, 0 si l'écart
     *         dépasse ce qu'on peut compenser sans déformation visible
     * @throws IllegalArgumentException si la correction contient un caractère
     *         absent de la police d'écriture
     */
    private float ajustement(PDFont origine, String texteOrigine,
                             PDFont ecriture, String texteCorrige) throws IOException {
        // getStringWidth lève une exception si un caractère est absent de la
        // police : cela vaut vérification que la correction est représentable.
        float largeurOrigine = origine.getStringWidth(texteOrigine);
        float largeurNouvelle = ecriture.getStringWidth(texteCorrige);
        if (largeurOrigine <= 0 || largeurNouvelle <= 0) return 1f;

        float ajustement = largeurOrigine / largeurNouvelle;
        return (ajustement < AJUSTEMENT_MIN || ajustement > AJUSTEMENT_MAX) ? 0f : ajustement;
    }

    /**
     * Police à employer pour écrire le texte corrigé : la police courante si
     * elle en a les glyphes, sinon une variante de la même famille.
     *
     * Les polices intégrées sont sous-ensemblées — elles ne contiennent que les
     * glyphes réellement employés dans le document. Corriger « précieu » en
     * « précieux » échoue donc si aucun mot en gras ne comporte de « x », alors
     * que la même famille en graisse normale, plus sollicitée, l'a presque
     * toujours. Le mot change alors de graisse, ce qui se remarque bien moins
     * qu'une faute laissée en place.
     *
     * @param seulDansLOperande le passage n'affiche que le mot fautif ; sinon
     *        changer de police déformerait aussi le texte qui l'entoure
     * @return la police à employer, ou null si aucune ne convient
     */
    private Police policeCapable(PDPage page, Police police, String texte, boolean seulDansLOperande) {
        try {
            police.fonte().getStringWidth(texte);
            return police;
        } catch (Exception e) {
            return seulDansLOperande ? substitut(page, police, texte) : null;
        }
    }

    /** Première police de la page, de la même famille, sachant écrire ce texte. */
    private Police substitut(PDPage page, Police origine, String texte) {
        for (COSName nom : page.getResources().getFontNames()) {
            if (nom.equals(origine.nom())) continue;
            try {
                PDFont candidate = page.getResources().getFont(nom);
                if (candidate == null || !famille(candidate).equals(famille(origine.fonte()))) continue;
                candidate.getStringWidth(texte); // lève si un glyphe manque
                return new Police(nom, origine.taille(), candidate);
            } catch (Exception e) {
                // cette police ne sait pas écrire le texte : on essaie la suivante
            }
        }
        return null;
    }

    /**
     * Famille d'une police, dépouillée du préfixe de sous-ensemble et de la
     * variante : « ABCDEE+Calibri,Bold » et « ABCDEE+Calibri » donnent « calibri ».
     */
    private String famille(PDFont police) {
        String nom = police.getName() == null ? "" : police.getName();
        int plus = nom.indexOf('+');
        if (plus >= 0) nom = nom.substring(plus + 1);
        int coupure = nom.length();
        for (char separateur : new char[]{',', '-'}) {
            int position = nom.indexOf(separateur);
            if (position >= 0) coupure = Math.min(coupure, position);
        }
        return nom.substring(0, coupure).toLowerCase();
    }

    /**
     * Encadre l'opérande de texte des opérateurs nécessaires : changement de
     * police lorsqu'un substitut a été retenu, mise à l'échelle horizontale pour
     * conserver la largeur d'origine. L'état antérieur est rétabli juste après
     * l'affichage, afin de ne pas déteindre sur la suite de la page.
     */
    private void encadrer(List<Object> resultat, Police origine, Police ecriture,
                          float echelleCourante, float ajustement) {
        List<Object> avant = new ArrayList<>();
        List<Object> apres = new ArrayList<>();

        if (!ecriture.nom().equals(origine.nom())) {
            avant.addAll(List.of(ecriture.nom(), new COSFloat(ecriture.taille()), Operator.getOperator("Tf")));
            apres.addAll(List.of(origine.nom(), new COSFloat(origine.taille()), Operator.getOperator("Tf")));
        }
        if (ajustement != 1f) {
            avant.addAll(List.of(new COSFloat(echelleCourante * ajustement), Operator.getOperator("Tz")));
            apres.addAll(List.of(new COSFloat(echelleCourante), Operator.getOperator("Tz")));
        }
        if (avant.isEmpty()) return;

        // resultat se termine par : … opérande, opérateur d'affichage
        resultat.addAll(resultat.size() - 2, avant);
        resultat.addAll(apres);
    }

    /**
     * Applique le remplacement dans un tableau TJ.
     *
     * Les traitements de texte y découpent les mots en fragments pour gérer le
     * crénage : « misterieuse » peut être stocké comme « m | i | st | eri | euse ».
     * On reconstitue donc le texte complet du tableau pour y chercher le mot,
     * puis on réécrit les fragments concernés.
     */
    private boolean remplacerDansTableau(PDPage page, COSArray tableau, Police police, String motFautif, Cible cible,
                                         String correction, List<Object> resultat, float echelleCourante) {
        if (police == null) return false;

        try {
            String texteOrigine = texteDuTableau(tableau, police.fonte());
            if (texteOrigine == null) return false;

            // Rangs, dans ce passage, des occurrences que la cible retient.
            List<Integer> aReecrire = new ArrayList<>();
            Matcher chercheur = motifDeMot(motFautif).matcher(texteOrigine);
            for (int rang = 0; chercheur.find(); rang++) {
                if (cible.retenir()) aReecrire.add(rang);
            }
            if (aReecrire.isEmpty()) return false;

            String texteCorrige = reecrire(texteOrigine, motFautif, correction, aReecrire);

            // Tout est décidé avant la moindre modification : si la correction
            // n'est pas représentable ou si l'écart de largeur est incompensable,
            // le tableau reste intact et la faute sera annotée plutôt que
            // réécrite de travers.
            Police ecriture = policeCapable(page, police, texteCorrige,
                                            texteOrigine.trim().equals(motFautif));
            if (ecriture == null) return false;

            float ajustement = ajustement(police.fonte(), texteOrigine, ecriture.fonte(), texteCorrige);
            if (ajustement == 0) return false;

            // Chaque réécriture fait disparaître l'occurrence traitée : le rang
            // des suivantes se décale d'autant.
            int dejaReecrites = 0;
            for (int rang : aReecrire) {
                if (!remplacerUneOccurrence(tableau, police.fonte(), ecriture.fonte(),
                                            motFautif, correction, rang - dejaReecrites)) break;
                dejaReecrites++;
            }
            if (dejaReecrites == 0) return false;

            encadrer(resultat, police, ecriture, echelleCourante, ajustement);
            return true;

        } catch (IllegalArgumentException | IOException e) {
            return false;
        }
    }

    /**
     * Remplace la première occurrence du mot dans le tableau, éventuellement à
     * cheval sur plusieurs fragments.
     *
     * @return true si une occurrence a été traitée
     */
    private boolean remplacerUneOccurrence(COSArray tableau, PDFont lecture, PDFont ecriture,
                                           String motFautif, String correction, int aSauter)
            throws IOException {
        StringBuilder texteComplet = new StringBuilder();
        List<int[]> origines = new ArrayList<>(); // par caractère : {position dans le tableau, position dans le fragment}

        for (int position = 0; position < tableau.size(); position++) {
            if (!(tableau.get(position) instanceof COSString fragment)) continue;
            String texte = decoder(fragment, lecture);
            if (texte == null) return false;
            for (int c = 0; c < texte.length(); c++) {
                origines.add(new int[]{position, c});
            }
            texteComplet.append(texte);
        }
        if (origines.isEmpty()) return false;

        Matcher chercheur = motifDeMot(motFautif).matcher(texteComplet);
        for (int passees = 0; passees <= aSauter; passees++) {
            if (!chercheur.find()) return false;
        }

        int debut = chercheur.start();
        int fin = chercheur.end();
        int premierFragment = origines.get(debut)[0];
        int dernierFragment = origines.get(fin - 1)[0];

        String texteDebut = decoder((COSString) tableau.get(premierFragment), lecture);
        String texteFin = decoder((COSString) tableau.get(dernierFragment), lecture);
        if (texteDebut == null || texteFin == null) return false;

        String avant = texteDebut.substring(0, origines.get(debut)[1]);
        String apres = texteFin.substring(origines.get(fin - 1)[1] + 1);

        if (premierFragment == dernierFragment) {
            ecrire(tableau, premierFragment, ecriture, avant + correction + apres);
        } else {
            ecrire(tableau, premierFragment, ecriture, avant + correction);
            ecrire(tableau, dernierFragment, ecriture, apres);
            // Fragments intermédiaires : leur contenu fait partie du mot réécrit.
            // Les ajustements de crénage internes sont neutralisés, le mot étant
            // désormais écrit d'un seul tenant.
            for (int position = premierFragment + 1; position < dernierFragment; position++) {
                if (tableau.get(position) instanceof COSString) {
                    ecrire(tableau, position, ecriture, "");
                } else if (tableau.get(position) instanceof COSNumber) {
                    tableau.set(position, new COSFloat(0));
                }
            }
        }
        return true;
    }

    private void ecrire(COSArray tableau, int position, PDFont police, String texte) throws IOException {
        tableau.set(position, new COSString(police.encode(texte)));
    }

    /** Texte reconstitué de tous les fragments du tableau, ou null si indéchiffrable. */
    private String texteDuTableau(COSArray tableau, PDFont police) {
        StringBuilder texte = new StringBuilder();
        for (COSBase element : tableau) {
            if (element instanceof COSString fragment) {
                String morceau = decoder(fragment, police);
                if (morceau == null) return null;
                texte.append(morceau);
            }
        }
        return texte.toString();
    }

    /** Applique la correction aux seules occurrences dont le rang est retenu. */
    private String reecrire(String texte, String motFautif, String correction, List<Integer> rangs) {
        StringBuilder resultat = new StringBuilder();
        Matcher chercheur = motifDeMot(motFautif).matcher(texte);

        int rang = 0;
        int repris = 0;
        while (chercheur.find()) {
            if (rangs.contains(rang)) {
                resultat.append(texte, repris, chercheur.start()).append(correction);
                repris = chercheur.end();
            }
            rang++;
        }
        return resultat.append(texte.substring(repris)).toString();
    }

    /**
     * Motif reconnaissant le mot entier. UNICODE_CHARACTER_CLASS est nécessaire
     * pour que les lettres accentuées comptent comme des caractères de mot.
     */
    private Pattern motifDeMot(String mot) {
        return Pattern.compile("\\b" + Pattern.quote(mot) + "\\b", Pattern.UNICODE_CHARACTER_CLASS);
    }

    /** Décode une chaîne du flux en texte lisible, selon la police active. */
    private String decoder(COSString chaine, PDFont police) {
        try (InputStream flux = new ByteArrayInputStream(chaine.getBytes())) {
            StringBuilder texte = new StringBuilder();
            while (flux.available() > 0) {
                int code = police.readCode(flux);
                String unicode = police.toUnicode(code);
                if (unicode == null) return null; // caractère non décodable : on renonce
                texte.append(unicode);
            }
            return texte.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /** Retrouve la police désignée par l'opérateur Tf courant, avec son corps. */
    private Police resoudrePolice(PDPage page, List<Object> jetons, int indexOperateur) {
        try {
            // Tf est précédé de : /NomPolice taille
            if (indexOperateur < 2) return null;
            if (jetons.get(indexOperateur - 2) instanceof COSName nom
                    && jetons.get(indexOperateur - 1) instanceof COSNumber taille) {
                PDFont fonte = page.getResources().getFont(nom);
                return fonte == null ? null : new Police(nom, taille.floatValue(), fonte);
            }
        } catch (Exception e) {
            log.debug("Police introuvable", e);
        }
        return null;
    }

    private void reecrireContenu(PDDocument document, PDPage page, List<Object> jetons) throws IOException {
        PDStream nouveauFlux = new PDStream(document);
        try (OutputStream sortie = nouveauFlux.createOutputStream(COSName.FLATE_DECODE)) {
            new ContentStreamWriter(sortie).writeTokens(jetons);
        }
        page.setContents(nouveauFlux);
    }

    // ─── Surlignage des fautes non corrigées ──────────────────────────────────

    /** Surligne en jaune chaque faute non réécrite, avec la correction en commentaire. */
    private void surligner(PDDocument document, PDPage page, int numeroPage, List<FauteDTO> fautes) {
        try {
            Map<String, List<ExtracteurTextePdf.PositionMot>> positions =
                    ExtracteurTextePdf.positionsDesMots(document, numeroPage);

            for (FauteDTO faute : fautes) {
                List<ExtracteurTextePdf.PositionMot> occurrences = positions.get(faute.getMotFautif());
                if (occurrences == null || occurrences.isEmpty()) continue;

                for (ExtracteurTextePdf.PositionMot position : occurrences) {
                    page.getAnnotations().add(construireSurlignage(position, faute));
                }
            }
        } catch (Exception e) {
            log.warn("Surlignage impossible sur la page {}", numeroPage, e);
        }
    }

    private PDAnnotationHighlight construireSurlignage(ExtracteurTextePdf.PositionMot position, FauteDTO faute) {
        PDAnnotationHighlight surlignage = new PDAnnotationHighlight();

        float bas = position.y() - position.hauteur() * 0.25f;
        float haut = position.y() + position.hauteur() * 0.85f;
        float gauche = position.x();
        float droite = position.x() + position.largeur();

        surlignage.setRectangle(new PDRectangle(gauche, bas, droite - gauche, haut - bas));
        // Les quadPoints décrivent la zone surlignée : coins haut-gauche, haut-droit,
        // bas-gauche, bas-droit (ordre imposé par la spécification PDF).
        surlignage.setQuadPoints(new float[]{
                gauche, haut, droite, haut,
                gauche, bas, droite, bas
        });
        surlignage.setColor(new PDColor(new float[]{1f, 0.92f, 0.23f}, PDDeviceRGB.INSTANCE));
        surlignage.setConstantOpacity(0.4f);

        String suggestion = faute.getCorrection() != null && !faute.getCorrection().isBlank()
                ? "Correction suggérée : " + faute.getCorrection()
                : "Aucune correction automatique disponible";
        surlignage.setContents(suggestion + "\n" + (faute.getMessage() == null ? "" : faute.getMessage()));

        return surlignage;
    }
}
