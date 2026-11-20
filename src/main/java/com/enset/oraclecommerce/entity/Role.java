package com.enset.oraclecommerce.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * @author pc
 **/
@Entity
@Table(name = "ROLE")
@Getter
@Setter
@NoArgsConstructor
public class Role {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "role_seq_generator"
    )
    @SequenceGenerator(
            name = "role_seq_generator",
            sequenceName = "SEQ_ROLE",
            allocationSize = 1
    )
    private Long id;

    @Column(name = "NOM", nullable = false, unique = true, length = 50)
    private String nom;

    @Column(name = "DESCRIPTION", length = 255)
    private String description;

    @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
    private Set<Utilisateur> utilisateurs = new HashSet<>();
}