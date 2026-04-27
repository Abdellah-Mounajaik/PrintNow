package com.printnow.module.shop.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalTime;

@Entity
@Table(name = "horaires_ouverture")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HoraireOuverture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "jour_semaine", length = 15)
    private String jourSemaine; // Ex: LUNDI, MARDI...

    @Column(name = "heure_ouverture")
    private LocalTime heureOuverture;

    @Column(name = "heure_fermeture")
    private LocalTime heureFermeture;

    private Boolean ferme;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_imprimerie", nullable = false)
    private Imprimerie imprimerie;
}