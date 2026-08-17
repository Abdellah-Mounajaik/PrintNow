package com.printnow.module.studio.model;

import com.printnow.module.order.model.Commande;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Un rendu concret d'une génération : le contenu structuré produit par l'IA,
 * plus les chemins du PDF et de son aperçu. La tranche verticale en crée une
 * seule par génération ; les « 3 propositions » viendront ensuite.
 */
@Entity
@Table(name = "propositions_support")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropositionSupport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generation_id", nullable = false)
    private GenerationSupport generation;

    @Column(nullable = false)
    private String gabaritCode;

    @Column(nullable = false)
    private String paletteCode;

    @Column(nullable = false)
    private String policeCode;

    /** Sortie de l'IA, éditable. Stockée en texte comme resultatAnalyse de la correction. */
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String contenuJson;

    private String cheminPdf;
    private String cheminApercu;

    @Column(nullable = false)
    private boolean choisie = false;

    /** Vraie une fois la génération payée avec une commande : évite de la facturer deux fois. */
    @Column(nullable = false)
    private boolean payee = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commande_id")
    private Commande commande;

    @CreationTimestamp
    private LocalDateTime dateCreation;
}
