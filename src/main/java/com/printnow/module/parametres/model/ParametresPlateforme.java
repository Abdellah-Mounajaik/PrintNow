package com.printnow.module.parametres.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Réglages tarifaires de la plateforme, modifiables par un administrateur sans
 * redéploiement.
 *
 * Table à une seule ligne (id toujours 1) : il n'existe qu'un jeu de tarifs
 * pour toute la plateforme, pas un par imprimerie.
 */
@Entity
@Table(name = "parametres_plateforme")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParametresPlateforme {

    @Id
    private Long id;

    /** Commission PrintNow, en pourcentage du TTC (ex: 10.00 pour 10%). */
    @Column(name = "commission_pourcentage", nullable = false)
    private BigDecimal commissionPourcentage;

    /** Frais d'inscription unique d'un imprimeur partenaire, en euros. */
    @Column(name = "frais_inscription", nullable = false)
    private BigDecimal fraisInscription;

    /** Forfait de correction orthographique couvrant les premières pages, en euros. */
    @Column(name = "prix_correction_forfait", nullable = false)
    private BigDecimal prixCorrectionForfait;

    /** Nombre de pages incluses dans le forfait de correction. */
    @Column(name = "pages_incluses_correction", nullable = false)
    private Integer pagesInclusesCorrection;

    /** Prix de chaque page de correction au-delà du forfait, en euros. */
    @Column(name = "prix_correction_page_supp", nullable = false)
    private BigDecimal prixCorrectionPageSupp;

    /** Prix d'un design généré par IA (studio) utilisé dans une commande, en euros. */
    @Column(name = "prix_generation_design", nullable = false)
    private BigDecimal prixGenerationDesign;

    /** Valeurs par défaut, utilisées si aucune ligne n'existe encore en base. */
    public static ParametresPlateforme valeursParDefaut() {
        return new ParametresPlateforme(
                1L,
                new BigDecimal("10.00"),
                new BigDecimal("100.00"),
                new BigDecimal("2.90"),
                10,
                new BigDecimal("0.20"),
                new BigDecimal("4.90")
        );
    }
}
