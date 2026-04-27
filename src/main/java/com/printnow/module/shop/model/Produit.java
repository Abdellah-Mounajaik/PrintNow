package com.printnow.module.shop.model;

import com.printnow.module.shop.enums.FormatImpression;
import com.printnow.module.shop.enums.TypeProduit;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    private Boolean actif;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_imprimerie", nullable = false)
    private Imprimerie imprimerie;
}
