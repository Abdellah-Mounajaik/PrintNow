package com.printnow.module.order.model;


import com.printnow.module.user.model.User;
import com.printnow.module.order.enums.ModeRetrait;
import com.printnow.module.order.enums.StatutCommande;
import com.printnow.module.shop.model.Imprimerie;
import com.printnow.module.promo.model.CodePromo;
// Importe tes autres entités (Paiement, AdresseLivraison, etc.) selon tes packages

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "commandes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Commande {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String numeroCommande;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutCommande statut;

    private LocalDateTime dateCreation;
    private LocalDateTime datePaiement;

    /**
     * Paiement Stripe correspondant à cette commande.
     *
     * Conservé pour pouvoir répondre à la question « cette somme encaissée
     * a-t-elle donné lieu à une commande ? » — sans quoi un paiement resté
     * orphelin serait indiscernable d'un paiement légitime.
     */
    @Column(name = "payment_intent_id", unique = true, length = 100)
    private String paymentIntentId;

    @Enumerated(EnumType.STRING)
    private ModeRetrait modeRetrait;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private Boolean express2h = false; 

    // --- Montants Financiers ---
    @Column(precision = 10, scale = 2)
    private BigDecimal totalHT;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalTVA;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalTTC;

    @Column(precision = 10, scale = 2)
    private BigDecimal commissionPlateforme;

    /**
     * Vérification orthographique réglée avec la commande.
     *
     * C'est un service de la plateforme, pas une prestation de l'imprimerie :
     * ce montant revient entièrement à PrintNow. Il est donc tenu à l'écart du
     * total de la commande, de l'assiette de la commission et de la somme versée
     * à l'imprimerie — il n'est enregistré ici que pour être comptabilisé.
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal montantCorrections;

    @Column(precision = 10, scale = 2)
    private BigDecimal montantVerseImprimerie;

    // Réduction liée au code promo (voir codePromo ci-dessous) — nom historique.
    @Column(precision = 10, scale = 2)
    private BigDecimal montantReduction;

    // Montants figés au moment de la commande (indépendants d'un futur
    // changement de tarif de l'imprimerie), pour un affichage fiable sur la facture.
    @Column(precision = 10, scale = 2)
    private BigDecimal fraisExpress;

    @Column(precision = 10, scale = 2)
    private BigDecimal fraisLivraison;

    @Column(precision = 10, scale = 2)
    private BigDecimal montantReductionEtudiant;

    // --- Relations Principales ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private User client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "imprimerie_id", nullable = false)
    private Imprimerie imprimerie;

    // CascadeType.ALL permet de sauvegarder les lignes en même temps que la commande
    @OneToMany(mappedBy = "commande", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LigneCommande> lignes = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "adresse_livraison_id")
    private AdresseLivraison adresseLivraison;

    @OneToOne(mappedBy = "commande", cascade = CascadeType.ALL, orphanRemoval = true)
    private Livraison livraison;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "code_promo_id")
    private CodePromo codePromo;

    // Méthode utilitaire pour ajouter une ligne de commande facilement
    public void addLigneCommande(LigneCommande ligne) {
        lignes.add(ligne);
        ligne.setCommande(this);
    }
}