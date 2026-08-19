package com.printnow.module.shop.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    /**
     * Identifiant lisible utilisé dans l'adresse de la fiche, dérivé du nom
     * (« Imprimerie du centre » → « imprimerie-du-centre »).
     *
     * Il est enregistré plutôt que recalculé à chaque affichage : rien n'impose
     * l'unicité des noms, et deux homonymes se disputeraient sinon la même
     * adresse. En cas de collision, un numéro est ajouté à la fin.
     */
    @Column(unique = true, length = 180)
    private String slug;

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

    @Column(name = "pourcentage_remise_recto_verso")
    private Integer pourcentageRemiseRectoVerso;

    @Column(columnDefinition = "boolean default false")
    private Boolean actif = false;;
    @OneToMany(mappedBy = "imprimerie", fetch = FetchType.LAZY)
    private List<HoraireOuverture> horaires;
    @OneToMany(mappedBy = "imprimerie", fetch = FetchType.LAZY)
    private List<Produit> produits;

    @Column(name = "numero_tva", unique = true, length = 50)
    private String numeroTva;

    // Champs dénormalisés — recalculés à chaque nouvel avis
    @Column(name = "note_moyenne")
    private Double noteMoyenne;

    @Column(name = "nombre_avis")
    private Integer nombreAvis = 0;

    // ─── Facture des frais d'inscription ─────────────────────────────────────
    /**
     * Numéro, date et montant figés au moment de l'inscription : les frais
     * d'inscription sont configurables (voir ParametresPlateforme), donc le
     * tarif effectivement facturé à cette imprimerie peut différer de celui en
     * vigueur aujourd'hui. La facture doit toujours refléter ce qui a réellement
     * été payé, jamais le tarif courant.
     */
    @Column(name = "numero_facture_inscription", length = 30)
    private String numeroFactureInscription;

    @Column(name = "date_inscription")
    private LocalDateTime dateInscription;

    @Column(name = "montant_frais_inscription", precision = 10, scale = 2)
    private BigDecimal montantFraisInscription;
}