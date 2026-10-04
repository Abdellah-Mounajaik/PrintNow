package com.printnow.module.user.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users") 
@AllArgsConstructor
@Data
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; 

    @Column(nullable = false, unique = true, length = 150)
    private String email; 

    @Column(name = "mot_de_passe", nullable = false)
    private String motDePasse; 

    @Column(length = 100)
    private String prenom; 

    @Column(length = 100)
    private String nom; 

    @Column(length = 20)
    private String telephone; 

    private Boolean actif;

    /**
     * Date à laquelle la suppression a été demandée, ou null si le compte est
     * bien vivant.
     *
     * La ligne est conservée plutôt que détruite : commandes, factures et avis
     * y renvoient, et la loi impose de garder les factures sept ans. Cette date
     * sert aussi à distinguer un compte supprimé d'un compte simplement
     * désactivé (un partenaire en attente de validation, par exemple).
     */
    @Column(name = "date_suppression")
    private LocalDateTime dateSuppression;

    /**
     * Date de l'effacement effectif des données personnelles, ou null tant que
     * le délai de rétractation court.
     *
     * La suppression se fait en deux temps : le compte est d'abord fermé, puis
     * anonymisé une fois le délai écoulé. Tant que cette date est nulle, un
     * administrateur peut encore rétablir le compte ; ensuite, plus rien ne
     * permet de revenir en arrière — c'est précisément le but de l'effacement.
     */
    @Column(name = "date_anonymisation")
    private LocalDateTime dateAnonymisation;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_role")
    private Role role;

    public boolean estSupprime() {
        return dateSuppression != null;
    }

    /** Suppression demandée, mais les données sont encore là : retour possible. */
    public boolean estRetablissable() {
        return dateSuppression != null && dateAnonymisation == null;
    }
}