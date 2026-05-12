package com.printnow.module.order.model;

import com.printnow.module.shop.model.Produit;
// Importe FichierPDF selon tes packages

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "lignes_commande")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LigneCommande {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer quantite;

    private Integer nbPages;
    private Boolean couleur;
    private Boolean rectoVerso;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal prixUnitaire;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal prixTotal;

    // --- Relations ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commande_id", nullable = false)
    private Commande commande;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produit_id", nullable = false)
    private Produit produit;

    @Column(name = "reliure")
    private String reliure;

    @Column(name = "finition")
    private String finition;
    /*
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "fichier_pdf_id")
    private FichierPDF fichierPdf;
    */
}