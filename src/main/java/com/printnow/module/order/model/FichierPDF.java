package com.printnow.module.order.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "fichiers_pdf")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FichierPDF {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nomFichier;

    @Column(nullable = false)
    private String cheminStockage;

    private Long tailleOctets;

    private Integer nbPagesDetectees;

    private LocalDateTime dateUpload;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ligne_commande_id", nullable = false)
    private LigneCommande ligneCommande;
}
