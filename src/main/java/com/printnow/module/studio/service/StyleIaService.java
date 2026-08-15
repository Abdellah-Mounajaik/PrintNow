package com.printnow.module.studio.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.printnow.module.studio.service.gabarit.Palette;
import com.printnow.module.studio.service.gabarit.Police;
import com.printnow.module.studio.service.gabarit.Style;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Choisit les 3 styles (couleurs + police) des 3 propositions.
 *
 * Les couleurs sont demandées à l'IA à partir du brief : si le client précise
 * des teintes (« gris et mauve »), la première palette les respecte, et l'espace
 * des couleurs possibles est infini plutôt que limité à une liste figée. On
 * borne puis on assombrit les couleurs reçues pour qu'elles restent lisibles sur
 * fond blanc. Si l'IA échoue ou renvoie moins de 3 palettes exploitables, on
 * complète avec les palettes figées ({@link Palette}).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StyleIaService {

    private static final String PROMPT_PALETTES = """
        Tu es directeur artistique. À partir de la description d'un support à
        imprimer, tu proposes 3 palettes de couleurs DISTINCTES et harmonieuses.

        Réponds UNIQUEMENT par un objet JSON valide, sans texte ni Markdown,
        exactement à ce format :
        {
          "palettes": [
            { "nom": "", "primaire": [0,0,0], "accent": [0,0,0], "texte": [0,0,0] }
          ]
        }
        (exactement 3 entrées dans "palettes")

        Règles :
        - Chaque composante est un entier entre 0 et 255 (Rouge, Vert, Bleu).
        - "primaire" = titres, "accent" = valeurs/sous-titres, "texte" = texte discret.
        - Le support est imprimé sur FOND BLANC : "primaire" et "accent" doivent
          rester FONCÉS et lisibles (évite les tons pâles). "texte" est un gris moyen.
        - "nom" est un libellé court de la palette (ex : « gris et mauve »).
        - Si la description mentionne des couleurs précises (ex : « gris et mauve »),
          la PREMIÈRE palette DOIT les respecter fidèlement.
        - Les 3 palettes doivent être nettement différentes les unes des autres.
        """;

    // Luminance perçue maximale (0..255) tolérée, pour rester lisible sur blanc.
    private static final double LUM_MAX_FORTE = 160;   // titres et accents
    private static final double LUM_MAX_TEXTE = 140;   // texte discret

    /** Une police par proposition ; distincte d'une proposition à l'autre. */
    private static final Police[] POLICES = {Police.MODERNE, Police.CLASSIQUE, Police.MODERNE};

    private final StudioIaService ia;
    private final ObjectMapper mapper;

    /** Trois styles pour les trois propositions, couleurs pilotées par le brief. */
    public List<Style> troisStyles(String brief) {
        List<Style> styles = new ArrayList<>();
        try {
            String json = ia.genererJson(PROMPT_PALETTES, brief);
            ReponsePalettes reponse = mapper.readValue(json, ReponsePalettes.class);
            if (reponse != null && reponse.palettes() != null) {
                for (PaletteProposee p : reponse.palettes()) {
                    if (styles.size() >= 3) break;
                    Style style = versStyle(p, POLICES[styles.size()]);
                    if (style != null) styles.add(style);
                }
            }
        } catch (Exception e) {
            log.warn("Palettes IA indisponibles, on retombe sur les palettes figées : {}", e.getMessage());
        }
        // Complète avec les palettes de secours si l'IA n'a pas fourni 3 palettes valides.
        for (Style secours : troisStylesDeSecours()) {
            if (styles.size() >= 3) break;
            styles.add(secours);
        }
        return styles;
    }

    private Style versStyle(PaletteProposee p, Police police) {
        if (p == null) return null;
        int[] primaire = assainir(p.primaire(), LUM_MAX_FORTE);
        int[] accent = assainir(p.accent(), LUM_MAX_FORTE);
        int[] texte = assainir(p.texte(), LUM_MAX_TEXTE);
        if (primaire == null || accent == null || texte == null) return null;
        return new Style(code(p.nom()), primaire, accent, texte, police);
    }

    /** Borne les composantes à 0..255 et assombrit la couleur si elle est trop claire. */
    private int[] assainir(int[] rgb, double lumMax) {
        if (rgb == null || rgb.length != 3) return null;
        double r = borne(rgb[0]), v = borne(rgb[1]), b = borne(rgb[2]);
        double lum = 0.2126 * r + 0.7152 * v + 0.0722 * b;
        if (lum > lumMax && lum > 0) {
            double facteur = lumMax / lum;
            r *= facteur;
            v *= facteur;
            b *= facteur;
        }
        return new int[]{(int) Math.round(r), (int) Math.round(v), (int) Math.round(b)};
    }

    private double borne(int composante) {
        return Math.max(0, Math.min(255, composante));
    }

    private String code(String nom) {
        if (nom == null || nom.isBlank()) return "ia";
        String code = nom.trim().toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return code.isBlank() ? "ia" : code;
    }

    /** Palettes figées : une version sobre garantie + 2 variées tirées au hasard. */
    private List<Style> troisStylesDeSecours() {
        List<Palette> variees = new ArrayList<>(Arrays.asList(Palette.values()));
        variees.remove(Palette.SOBRE);
        Collections.shuffle(variees);
        List<Style> styles = new ArrayList<>();
        styles.add(Style.de(Palette.SOBRE, Police.MODERNE));
        styles.add(Style.de(variees.get(0), Police.CLASSIQUE));
        styles.add(Style.de(variees.get(1), Police.MODERNE));
        return styles;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ReponsePalettes(List<PaletteProposee> palettes) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PaletteProposee(String nom, int[] primaire, int[] accent, int[] texte) {}
}
