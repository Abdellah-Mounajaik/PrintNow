package com.printnow.module.user.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_role")
    private Role role; 
}