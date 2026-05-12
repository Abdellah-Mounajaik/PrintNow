package com.printnow.module.order.model;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import com.printnow.module.order.enums.StatutLivraison;

@Entity
@Table(name = "livraison")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Livraison {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_suivi", unique = true)
    private String numeroSuivi;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_livraison", nullable = false)
    private StatutLivraison statutLivraison = StatutLivraison.EN_PREPARATION;

    @Column(name = "etiquette_url")
    private String etiquetteUrl; // L'URL vers le PDF de l'étiquette (ex: DHL, Bpost)

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_livraison")
    private LocalDateTime dateLivraison;

    // Relation (0..1) avec la Commande. C'est souvent la Livraison qui porte la clé étrangère.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commande_id")
    private Commande commande;

    @PrePersist
    protected void onCreate() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
    }
}