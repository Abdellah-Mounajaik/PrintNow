package com.printnow.module.studio.model;

import com.printnow.module.studio.enums.StatutGeneration;
import com.printnow.module.studio.enums.TypeSupport;
import com.printnow.module.user.model.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Une demande de génération : le brief du client et le type de support voulu.
 * Hibernate crée la table à partir de cette entité (ddl-auto=update).
 */
@Entity
@Table(name = "generations_support")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenerationSupport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private User client;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeSupport typeSupport;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String brief;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutGeneration statut;

    @Column(nullable = false, unique = true)
    private String suivi;

    @CreationTimestamp
    private LocalDateTime dateCreation;

    @UpdateTimestamp
    private LocalDateTime dateMaj;

    @OneToMany(mappedBy = "generation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PropositionSupport> propositions = new ArrayList<>();
}
