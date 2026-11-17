package com.enset.oraclecommerce.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * @author pc
 **/
@Entity
@Table(name = "UTILISATEUR")
@Getter
@Setter
@NoArgsConstructor
public class Utilisateur {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "utilisateur_seq_generator"
    )
    @SequenceGenerator(
            name = "utilisateur_seq_generator",
            sequenceName = "SEQ_UTILISATEUR",
            allocationSize = 1
    )
    private Long id;

    @Column(name = "NOM_UTILISATEUR", nullable = false, unique = true, length = 50)
    private String nomUtilisateur;

    @Column(name = "EMAIL", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "MOT_DE_PASSE", nullable = false, length = 255)
    private String motDePasse;

    @Column(name = "NOM", length = 100)
    private String nom;

    @Column(name = "PRENOM", length = 100)
    private String prenom;

    @Column(name = "ACTIF", nullable = false)
    private Boolean actif = true;

    @Column(name = "DATE_CREATION", nullable = false)
    private LocalDateTime dateCreation;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "UTILISATEUR_ROLE",
            joinColumns = @JoinColumn(name = "UTILISATEUR_ID"),
            inverseJoinColumns = @JoinColumn(name = "ROLE_ID")
    )
    private Set<Role> roles = new HashSet<>();

    @PrePersist
    public void prePersist() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
    }
}