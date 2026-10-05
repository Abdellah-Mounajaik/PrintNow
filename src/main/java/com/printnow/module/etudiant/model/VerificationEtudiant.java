package com.printnow.module.etudiant.model;

import com.printnow.module.etudiant.enums.StatutEtudiant;
import com.printnow.module.etudiant.enums.VerdictIA;
import com.printnow.module.user.model.User;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "verifications_etudiants")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerificationEtudiant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutEtudiant statut = StatutEtudiant.EN_ATTENTE;

    @Column(name = "carte_etudiante_path")
    private String carteEtudiantePath;

    @Column(name = "carte_identite_path")
    private String carteIdentitePath;

    @Column(name = "date_soumission")
    private LocalDateTime dateSoumission;

    @Column(name = "date_validation")
    private LocalDateTime dateValidation;

    @Column(name = "valable_jusqu_a")
    private LocalDateTime valableJusquA;

    @Column(name = "motif_refus", columnDefinition = "TEXT")
    private String motifRefus;

    // ─── Analyse automatique (IA) ────────────────────────────────────────────
    /** Verdict de la dernière analyse automatique. Null si pas encore analysée ou si l'appel a échoué. */
    @Enumerated(EnumType.STRING)
    @Column(name = "verdict_ia")
    private VerdictIA verdictIa;

    /** Nom lu par l'IA sur la carte étudiante. */
    @Column(name = "nom_extrait_carte_etudiante")
    private String nomExtraitCarteEtudiante;

    /** Nom lu par l'IA sur la carte d'identité. */
    @Column(name = "nom_extrait_carte_identite")
    private String nomExtraitCarteIdentite;

    /** True si le statut final (ACCEPTE/REFUSE) a été décidé par l'IA, sans intervention d'un admin. */
    @Column(name = "decision_automatique", nullable = false)
    private boolean decisionAutomatique = false;

    /**
     * Nombre de refus essuyés d'affilée. Au-delà de {@link #MAX_TENTATIVES}, les
     * resoumissions sont bloquées — sans cette limite, chaque refus (auto ou
     * manuel) rouvrait la porte à un nouvel essai indéfiniment.
     */
    @Column(name = "nombre_refus", nullable = false)
    private int nombreRefus = 0;

    /**
     * Au-delà de ce nombre de refus d'affilée, plus aucune resoumission n'est
     * acceptée : sans limite, un compte refusé pouvait retenter indéfiniment
     * (spam de demandes, et donc d'appels à l'IA qui les analyse).
     */
    public static final int MAX_TENTATIVES = 3;

    /**
     * Une nouvelle tentative est-elle encore permise ?
     *
     * Le service refuse déjà la soumission ; cette méthode existe pour que
     * l'écran du client sache s'il peut encore proposer le formulaire, sans
     * avoir à recompter les refus de son côté — la règle resterait alors écrite
     * à deux endroits.
     */
    public boolean isPeutResoumettre() {
        return statut != StatutEtudiant.REFUSE
                || nombreRefus < MAX_TENTATIVES
                || refusDUneAnneePassee();
    }

    /**
     * Les refus comptés datent-ils d'une année académique révolue ?
     *
     * La limite vise le spam de demandes dans l'année ; la reconduire d'une
     * année sur l'autre reviendrait à bannir à vie quelqu'un dont les photos
     * étaient mauvaises en première année, alors que sa vérification doit de
     * toute façon être refaite chaque année avec une nouvelle carte.
     */
    public boolean refusDUneAnneePassee() {
        return dateValidation != null
                && dateValidation.toLocalDate().isBefore(debutAnneeAcademique(LocalDate.now()));
    }

    /**
     * Dernier jour de l'année académique en cours : le 30 juin, date à laquelle
     * une vérification cesse de valoir et où les refus cessent de compter.
     */
    public static LocalDate finAnneeAcademique(LocalDate jour) {
        LocalDate trenteJuin = LocalDate.of(jour.getYear(), 6, 30);
        return jour.isAfter(trenteJuin) ? trenteJuin.plusYears(1) : trenteJuin;
    }

    /** Premier jour de l'année académique en cours, soit le 1er juillet précédent. */
    private static LocalDate debutAnneeAcademique(LocalDate jour) {
        return finAnneeAcademique(jour).minusYears(1).plusDays(1);
    }
}
