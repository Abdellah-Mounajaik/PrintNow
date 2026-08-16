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
 * Choisit les 3 styles (couleurs + fond + police) des 3 propositions.
 *
 * Les couleurs et le fond sont demandés à l'IA d'après le brief : si le client
 * précise des teintes ou un fond (« gris et mauve », « fond noir »), ils sont
 * respectés et appliqués aux 3 propositions. Le contraste est ensuite garanti
 * par les jetons du {@link Style} (couleurs claires sur fond sombre, foncées sur
 * fond clair). Si l'IA échoue, on retombe sur les palettes figées, page blanche.
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
            { "nom": "", "fond": [255,255,255], "fondImpose": false,
              "primaire": [0,0,0], "accent": [0,0,0], "texte": [0,0,0] }
          ]
        }
        (exactement 3 entrées dans "palettes")

        Règles :
        - Chaque composante est un entier entre 0 et 255 (Rouge, Vert, Bleu).
        - "primaire" = titres, "accent" = valeurs/sous-titres, "texte" = texte discret.
        - "fond" = couleur de fond du support. "primaire", "accent" et "texte"
          doivent être clairement LISIBLES sur ce "fond".
        - "fondImpose" = true UNIQUEMENT si la description demande explicitement une
          couleur de fond (ex : « fond noir », « sur fond crème »). Dans ce cas, mets
          la MÊME couleur "fond" (celle demandée) dans les 3 palettes.
        - Sinon "fondImpose" = false et "fond" = [255,255,255].
        - Si la description mentionne des couleurs précises (ex : « gris et mauve »),
          la PREMIÈRE palette DOIT les respecter fidèlement.
        - Les 3 palettes doivent être nettement différentes les unes des autres.
        """;

    /** Une police par proposition ; distincte d'une proposition à l'autre. */
    private static final Police[] POLICES = {Police.MODERNE, Police.CLASSIQUE, Police.MODERNE};

    private static final int[] BLANC = {255, 255, 255};

    private final StudioIaService ia;
    private final ObjectMapper mapper;

    /** Trois styles pour les trois propositions, couleurs et fond pilotés par le brief. */
    public List<Style> troisStyles(String brief) {
        List<Combo> combos = new ArrayList<>();
        int[] fondCommun = BLANC;
        boolean impose = false;

        try {
            String json = ia.genererJson(PROMPT_PALETTES, brief);
            ReponsePalettes reponse = mapper.readValue(json, ReponsePalettes.class);
            if (reponse != null && reponse.palettes() != null) {
                for (PaletteProposee p : reponse.palettes()) {
                    if (!impose && Boolean.TRUE.equals(p.fondImpose())) {
                        int[] f = borner(p.fond());
                        if (f != null) {
                            impose = true;
                            fondCommun = f;
                        }
                    }
                    Combo combo = versCombo(p);
                    if (combo != null) combos.add(combo);
                }
            }
        } catch (Exception e) {
            log.warn("Palettes IA indisponibles, on retombe sur les palettes figées : {}", e.getMessage());
        }

        // Complète avec les palettes de secours si moins de 3 combinaisons valides.
        for (Combo secours : combosDeSecours()) {
            if (combos.size() >= 3) break;
            combos.add(secours);
        }

        List<Style> styles = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Combo c = combos.get(i);
            styles.add(new Style(c.code(), fondCommun, impose, c.primaire(), c.accent(), c.texte(), POLICES[i]));
        }
        return styles;
    }

    private Combo versCombo(PaletteProposee p) {
        if (p == null) return null;
        int[] primaire = borner(p.primaire());
        int[] accent = borner(p.accent());
        int[] texte = borner(p.texte());
        if (primaire == null || accent == null || texte == null) return null;
        return new Combo(code(p.nom()), primaire, accent, texte);
    }

    /** Borne les composantes à 0..255, sans toucher à la teinte (le contraste est géré ailleurs). */
    private int[] borner(int[] rgb) {
        if (rgb == null || rgb.length != 3) return null;
        return new int[]{borne(rgb[0]), borne(rgb[1]), borne(rgb[2])};
    }

    private int borne(int composante) {
        return Math.max(0, Math.min(255, composante));
    }

    private String code(String nom) {
        if (nom == null || nom.isBlank()) return "ia";
        String code = nom.trim().toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return code.isBlank() ? "ia" : code;
    }

    /** Combinaisons figées : une version sobre garantie + 2 variées tirées au hasard. */
    private List<Combo> combosDeSecours() {
        List<Palette> variees = new ArrayList<>(Arrays.asList(Palette.values()));
        variees.remove(Palette.SOBRE);
        Collections.shuffle(variees);
        List<Combo> combos = new ArrayList<>();
        combos.add(combo(Palette.SOBRE));
        combos.add(combo(variees.get(0)));
        combos.add(combo(variees.get(1)));
        return combos;
    }

    private Combo combo(Palette p) {
        return new Combo(p.code(), p.primaire(), p.accent(), p.texte());
    }

    private record Combo(String code, int[] primaire, int[] accent, int[] texte) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ReponsePalettes(List<PaletteProposee> palettes) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PaletteProposee(String nom, int[] fond, Boolean fondImpose, int[] primaire, int[] accent, int[] texte) {}
}
