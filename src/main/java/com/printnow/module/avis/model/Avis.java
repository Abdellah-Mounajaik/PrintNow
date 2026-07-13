package com.printnow.module.avis.model;

import com.printnow.module.shop.model.Imprimerie;
import com.printnow.module.user.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "avis",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "imprimerie_id"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Avis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "imprimerie_id", nullable = false)
    private Imprimerie imprimerie;

    @Column(nullable = false)
    private Integer note; // 1 à 5

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;

    @PrePersist
    protected void onCreate() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
    }
}
