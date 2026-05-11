package com.printnow.module.shop.model;

import com.printnow.module.shop.enums.FormatImpression;
import com.printnow.module.shop.enums.TypeProduit;
import com.printnow.module.shop.enums.TypePlastification;
import com.printnow.module.shop.enums.TypeReliure;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map; // <-- N'oublie pas cet import !

@Entity
@Table(name = "produits")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Produit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_produit", length = 50)
    private TypeProduit typeProduit;

    @Enumerated(EnumType.STRING)
    @Column(name = "format_impression", length = 50)
    private FormatImpression formatImpression;

    @Column(name = "prix_base")
    private Double prixBase;

    @Column(name = "prix_par_page")
    private Double prixParPage;

    // ==========================================
    // OPTIONS DE PLASTIFICATION (Map : Type -> Prix)
    // ==========================================
    @Column(name = "propose_plastification")
    private Boolean proposePlastification = false;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "produit_prix_plastification", joinColumns = @JoinColumn(name = "produit_id"))
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "type_plastification")
    @Column(name = "prix")
    private Map<TypePlastification, Double> prixParTypePlastification;

    // ==========================================
    // OPTIONS DE RELIURE (Map : Type -> Prix)
    // ==========================================
    @Column(name = "propose_reliure")
    private Boolean proposeReliure = false;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "produit_prix_reliure", joinColumns = @JoinColumn(name = "produit_id"))
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "type_reliure")
    @Column(name = "prix")
    private Map<TypeReliure, Double> prixParTypeReliure;

    // ==========================================
    // STATUT ET RELATIONS
    // ==========================================
    private Boolean actif;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_imprimerie", nullable = false)
    private Imprimerie imprimerie;
}