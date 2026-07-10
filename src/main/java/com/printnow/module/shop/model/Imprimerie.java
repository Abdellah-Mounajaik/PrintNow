package com.printnow.module.shop.model;

import java.util.List;

import com.printnow.module.user.model.User;

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

    @OneToOne
    @JoinColumn(name = "id_gerant", nullable = false)
    private User gerant;

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

    @Column(name = "prix_livraison")
    private Double prixLivraison;

    private Boolean proposeTarifEtudiant;

    @Column(name = "pourcentage_remise_etudiant")
    private Integer pourcentageRemiseEtudiant;

    @Column(columnDefinition = "boolean default false")
    private Boolean actif = false;;
    @OneToMany(mappedBy = "imprimerie", fetch = FetchType.LAZY)
    private List<HoraireOuverture> horaires;
    @OneToMany(mappedBy = "imprimerie", fetch = FetchType.LAZY)
    private List<Produit> produits;

    @Column(name = "numero_tva", unique = true, length = 50)
    private String numeroTva;
}