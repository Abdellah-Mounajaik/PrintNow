package com.printnow.module.promo.model;

import com.printnow.module.promo.enums.TypeReduction;
import com.printnow.module.shop.model.Imprimerie;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "codes_promo")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodePromo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeReduction typeReduction;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal valeurReduction;

    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;

    private Integer utilisationMax;

    @Column(columnDefinition = "integer default 0")
    private Integer utilisationCourante = 0;

    @Column(precision = 10, scale = 2)
    private BigDecimal montantMinimumCommande;

    @Column(columnDefinition = "boolean default true")
    private Boolean actif = true;

    @Column(columnDefinition = "boolean default false")
    private Boolean supprime = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "imprimerie_id")
    private Imprimerie imprimerie;
}
