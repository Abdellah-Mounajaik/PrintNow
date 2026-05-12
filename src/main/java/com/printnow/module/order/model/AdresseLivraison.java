package com.printnow.module.order.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "adresse_livraison")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdresseLivraison {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nom_destinataire", nullable = false)
    private String nomDestinataire;

    @Column(nullable = false)
    private String rue;

    @Column(nullable = false)
    private String numero;

    @Column(name = "code_postal", nullable = false)
    private String codePostal;

    @Column(nullable = false)
    private String ville;

    @Column(nullable = false)
    private String pays;

    @Column(nullable = false)
    private String telephone;

    // Coordonnées optionnelles (si tu veux afficher une carte pour le livreur)
    private Double latitude;
    private Double longitude;

    /*
     * Note: Dans ton diagramme, une Commande a 0 ou 1 AdresseLivraison.
     * La relation est généralement portée par la Commande. 
     * Si tu as besoin du lien bidirectionnel ici, décommente la ligne ci-dessous :
     */
    // @OneToOne(mappedBy = "adresseLivraison")
    // private Commande commande;
}