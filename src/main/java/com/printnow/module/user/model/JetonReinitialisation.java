package com.printnow.module.user.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Autorisation temporaire de choisir un nouveau mot de passe sans connaître
 * l'ancien.
 *
 * Seule l'empreinte du jeton est conservée : le jeton lui-même ne circule que
 * dans le lien envoyé par email. Quelqu'un qui lirait cette table ne pourrait
 * donc pas reconstituer les liens en cours et s'emparer des comptes.
 */
@Entity
@Table(name = "jetons_reinitialisation")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JetonReinitialisation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Empreinte SHA-256 du jeton, jamais le jeton en clair. */
    @Column(name = "empreinte", nullable = false, unique = true, length = 64)
    private String empreinte;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_user", nullable = false)
    private User utilisateur;

    @Column(name = "cree_le", nullable = false)
    private LocalDateTime creeLe;

    @Column(name = "expire_le", nullable = false)
    private LocalDateTime expireLe;

    /** Renseigné dès que le jeton a servi : il ne peut plus resservir. */
    @Column(name = "utilise_le")
    private LocalDateTime utiliseLe;

    public boolean estUtilisable(LocalDateTime maintenant) {
        return utiliseLe == null && maintenant.isBefore(expireLe);
    }
}
