package com.printnow.infrastructure.seo;

import com.printnow.module.shop.repository.ImprimerieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Génère le sitemap.xml à la volée plutôt qu'un fichier statique : les fiches
 * imprimerie (/imprimerie/{slug}) sont créées dynamiquement, un fichier figé
 * dans le frontend ne pourrait jamais les lister sans être régénéré à chaque
 * inscription/fermeture de partenaire.
 */
@RestController
@RequiredArgsConstructor
public class SitemapController {

    private final ImprimerieRepository imprimerieRepository;

    @Value("${printnow.site.url:https://printnow.be}")
    private String siteUrl;

    private static final String[] PAGES_STATIQUES = {
            "/", "/imprimeries", "/devenir-partenaire", "/faq", "/contact",
            "/mentions-legales", "/conditions-generales", "/confidentialite"
    };

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> sitemap() {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        for (String page : PAGES_STATIQUES) {
            ajouterUrl(xml, siteUrl + page, "weekly");
        }

        imprimerieRepository.findAllByActifTrue().forEach(imprimerie -> {
            if (imprimerie.getSlug() != null) {
                ajouterUrl(xml, siteUrl + "/imprimerie/" + imprimerie.getSlug(), "weekly");
            }
        });

        xml.append("</urlset>\n");

        return ResponseEntity.ok().contentType(MediaType.APPLICATION_XML).body(xml.toString());
    }

    private void ajouterUrl(StringBuilder xml, String loc, String changefreq) {
        xml.append("  <url>\n")
                .append("    <loc>").append(loc).append("</loc>\n")
                .append("    <changefreq>").append(changefreq).append("</changefreq>\n")
                .append("  </url>\n");
    }
}
