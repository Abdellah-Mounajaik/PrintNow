package com.printnow.module.shop.model;

import com.printnow.module.shop.enums.FormatImpression;
import com.printnow.module.shop.enums.TypeProduit;
import com.printnow.module.shop.enums.TypePlastification;
import com.printnow.module.shop.enums.TypeReliure;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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
    // OPTIONS DE PLASTIFICATION
    // ==========================================
    @Column(name = "propose_plastification")
    private Boolean proposePlastification = false;

    @Column(name = "prix_plastification")
    private Double prixPlastification;

    @ElementCollection(targetClass = TypePlastification.class, fetch = FetchType.LAZY)
    @CollectionTable(name = "produit_plastifications", joinColumns = @JoinColumn(name = "produit_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "type_plastification")
    private List<TypePlastification> typesPlastification;

    // ==========================================
    // OPTIONS DE RELIURE
    // ==========================================
    @Column(name = "propose_reliure")
    private Boolean proposeReliure = false;

    @Column(name = "prix_reliure")
    private Double prixReliure;

    @ElementCollection(targetClass = TypeReliure.class, fetch = FetchType.LAZY)
    @CollectionTable(name = "produit_reliures", joinColumns = @JoinColumn(name = "produit_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "type_reliure")
    private List<TypeReliure> typesReliure;

    // ==========================================
    // STATUT ET RELATIONS
    // ==========================================
    private Boolean actif;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_imprimerie", nullable = false)
    private Imprimerie imprimerie;
}