package com.printnow.module.correction.model;

import com.printnow.module.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Une demande de vérification orthographique d'un PDF.
 *
 * L'analyse est réalisée gratuitement à la création (on ne renvoie alors que le
 * nombre de fautes), mais le détail et le PDF corrigé ne sont accessibles
 * qu'une fois le paiement confirmé. Le résultat brut de LanguageTool est donc
 * conservé ici pour ne pas relancer l'analyse après le paiement.
 */
@Entity
@Table(name = "verifications_orthographe")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VerificationOrthographe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private User client;

    private String nomFichier;

    @Column(nullable = false)
    private String cheminOriginal;

    /** Renseigné seulement après paiement, quand le PDF corrigé est généré. */
    private String cheminCorrige;

    private Integer nbPages;

    private Integer nbFautes;

    /**
     * Langue reconnue dans le document, au format LanguageTool (« fr », « nl »,
     * « en-US »). Conservée pour que le client sache dans quelle langue son
     * document a été relu.
     */
    private String langue;

    /** Nombre de fautes réellement corrigées dans le PDF (les autres sont annotées). */
    private Integer nbCorrigees;

    @Column(precision = 10, scale = 2)
    private BigDecimal prix;

    @Column(nullable = false)
    private Boolean payee = false;

    /** Résultat brut de LanguageTool, conservé pour être exploité après paiement. */
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String resultatAnalyse;

    private LocalDateTime dateCreation;

    private LocalDateTime datePaiement;
}
