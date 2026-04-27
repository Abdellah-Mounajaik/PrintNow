package com.printnow.module.shop.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "imprimeries")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Imprimerie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_gerant")
    private Long idGerant; // Lien avec l'utilisateur (gérant)

    @Column(nullable = false, length = 150)
    private String nom;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "logo_url", length = 255)
    private String logoUrl;

    @Column(name = "email_contact", length = 150)
    private String emailContact;

    @Column(name = "telephone_contact", length = 20)
    private String telephoneContact;

    @Column(columnDefinition = "TEXT")
    private String adresse;

    @Column(length = 100)
    private String ville;

    @Column(length = 100)
    private String pays;

    private Double latitude;
    private Double longitude;

    @Column(name = "propose_express_2h")
    private Boolean proposeExpress2h;

    @Column(name = "prix_express_2h")
    private Double prixExpress2h;

    @Column(name = "livraison_active")
    private Boolean livraisonActive;

    private Boolean actif;
}