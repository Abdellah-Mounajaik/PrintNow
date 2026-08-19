package com.printnow.module.etudiant.model;

import com.printnow.module.etudiant.enums.StatutEtudiant;
import com.printnow.module.etudiant.enums.VerdictIA;
import com.printnow.module.user.model.User;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

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
}
