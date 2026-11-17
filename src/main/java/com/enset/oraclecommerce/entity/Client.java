package com.enset.oraclecommerce.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * @author pc
 **/
@Entity
@Table(name="CLIENT")
@Getter @Setter
@NoArgsConstructor
public class Client {
    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "client_seq_generator"
    )
    @SequenceGenerator(
            name = "client_seq_generator",
            sequenceName = "SEQ_CLIENT",
            allocationSize = 1
    )
    private Long id;

    @Column(name = "NOM", nullable = false, length = 100)
    private String nom;

    @Column(name = "PRENOM", nullable = false, length = 100)
    private String prenom;

    @Column(name = "EMAIL", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "TELEPHONE", length = 20)
    private String telephone;

    @Column(name = "ADRESSE", length = 255)
    private String adresse;

    @Column(name = "DATE_CREATION", nullable = false)
    private LocalDateTime dateCreation;

    @PrePersist
    public void prePersist() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
    }

}
