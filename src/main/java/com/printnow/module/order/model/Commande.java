package com.printnow.module.order.model;


import com.printnow.module.user.model.User;
import com.printnow.module.order.enums.ModeRetrait;
import com.printnow.module.order.enums.StatutCommande;
import com.printnow.module.shop.model.Imprimerie;
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

    @Column(precision = 10, scale = 2)
    private BigDecimal montantVerseImprimerie;

    @Column(precision = 10, scale = 2)
    private BigDecimal montantReduction;

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
    // --- Autres Relations (à décommenter/adapter quand tu auras créé ces classes) ---
    /*
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "code_promo_id")
    private CodePromo codePromo;



    @OneToOne(mappedBy = "commande")
    private Paiement paiement;

    @OneToMany(mappedBy = "commande")
    private List<Facture> factures = new ArrayList<>();
    */

    // Méthode utilitaire pour ajouter une ligne de commande facilement
    public void addLigneCommande(LigneCommande ligne) {
        lignes.add(ligne);
        ligne.setCommande(this);
    }
}