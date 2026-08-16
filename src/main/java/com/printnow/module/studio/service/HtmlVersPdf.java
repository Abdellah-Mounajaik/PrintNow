package com.printnow.module.studio.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

/**
 * Convertit une page XHTML/CSS en PDF (via OpenHTMLtoPDF, bâti sur PDFBox 3).
 *
 * Le contenu doit être un XHTML bien formé (balises fermées, attributs entre
 * guillemets, entités échappées) : le moteur le parse en XML. La taille de page
 * et les marges se définissent dans le CSS via {@code @page}.
 */
@Service
public class HtmlVersPdf {

    public byte[] versPdf(String xhtml) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(xhtml, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Échec de la conversion HTML→PDF", e);
        }
    }
}
