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
     * Date à laquelle le compte a été supprimé, ou null s'il est bien vivant.
     *
     * La ligne est conservée plutôt que détruite : commandes, factures et avis
     * y renvoient, et la loi impose de garder les factures sept ans. Les données
     * personnelles, elles, sont effacées au moment de la suppression — c'est ce
     * que le RGPD exige, et cette date sert à distinguer un compte supprimé d'un
     * compte simplement désactivé (un partenaire en attente de validation, par
     * exemple).
     */
    @Column(name = "date_suppression")
    private LocalDateTime dateSuppression;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_role")
    private Role role;

    public boolean estSupprime() {
        return dateSuppression != null;
    }
}