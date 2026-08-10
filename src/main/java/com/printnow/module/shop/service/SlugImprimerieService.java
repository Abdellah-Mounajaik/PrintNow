package com.printnow.module.shop.service;

import com.printnow.module.shop.model.Imprimerie;
import com.printnow.module.shop.repository.ImprimerieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/**
 * Fabrique l'adresse lisible d'une fiche imprimerie à partir de son nom.
 *
 * « Imprimerie du Centre » devient « imprimerie-du-centre », de sorte que
 * l'adresse dise à qui elle mène plutôt que d'afficher un numéro. Le résultat
 * est enregistré sur l'imprimerie : rien n'impose l'unicité des noms, et
 * recalculer à la volée ferait pointer deux homonymes vers la même page.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SlugImprimerieService {

    /** Repli lorsque le nom ne contient aucun caractère utilisable (« ??? »). */
    private static final String SLUG_PAR_DEFAUT = "imprimerie";

    private final ImprimerieRepository imprimerieRepository;

    /**
     * Attribue son adresse lisible à l'imprimerie, en repartant de son nom.
     *
     * Ne fait rien si le nom n'a pas changé : les adresses déjà partagées
     * continuent ainsi de fonctionner tant que l'imprimerie garde son nom.
     */
    public void attribuer(Imprimerie imprimerie) {
        String souhaite = slugifier(imprimerie.getNom());
        if (souhaite.equals(imprimerie.getSlug())) return;

        imprimerie.setSlug(rendreUnique(souhaite, imprimerie.getId()));
    }

    /** Réduit un texte libre à des minuscules non accentuées séparées par des tirets. */
    public String slugifier(String texte) {
        if (texte == null || texte.isBlank()) return SLUG_PAR_DEFAUT;

        String sansAccents = Normalizer.normalize(texte, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String slug = sansAccents.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");

        return slug.isBlank() ? SLUG_PAR_DEFAUT : slug;
    }

    /**
     * Complète le slug d'un numéro tant qu'une autre imprimerie le porte déjà :
     * deux « Imprimerie du centre » deviennent « imprimerie-du-centre » et
     * « imprimerie-du-centre-2 ».
     */
    private String rendreUnique(String souhaite, Long idActuel) {
        String candidat = souhaite;
        int suffixe = 1;

        while (estPris(candidat, idActuel)) {
            suffixe++;
            candidat = souhaite + "-" + suffixe;
        }
        return candidat;
    }

    private boolean estPris(String slug, Long idActuel) {
        return imprimerieRepository.findBySlug(slug)
                .filter(autre -> !autre.getId().equals(idActuel))
                .isPresent();
    }

    /**
     * Donne son adresse lisible à chaque imprimerie qui n'en a pas encore.
     *
     * Les imprimeries créées avant la mise en place du slug n'en portent aucun ;
     * sans ce rattrapage, leur fiche resterait inatteignable.
     */
    @Bean
    ApplicationRunner completerLesSlugsManquants() {
        return arguments -> {
            List<Imprimerie> sansSlug = imprimerieRepository.findBySlugIsNull();
            if (sansSlug.isEmpty()) return;

            for (Imprimerie imprimerie : sansSlug) {
                imprimerie.setSlug(rendreUnique(slugifier(imprimerie.getNom()), imprimerie.getId()));
                imprimerieRepository.save(imprimerie);
            }
            log.info("Adresse lisible attribuée à {} imprimerie(s)", sansSlug.size());
        };
    }
}
