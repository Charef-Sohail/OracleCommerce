package com.enset.oraclecommerce.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author pc
 **/
@Entity
@Table(name = "CATEGORIE")
@Getter
@Setter
@NoArgsConstructor
public class Categorie {
    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "categorie_seq_generator"
    )
    @SequenceGenerator(
            name = "categorie_seq_generator",
            sequenceName = "SEQ_CATEGORIE",
            allocationSize = 1
    )
    private Long id;

    @Column(name = "NOM", nullable = false, unique = true, length = 100)
    private String nom;

    @Column(name = "DESCRIPTION", length = 500)
    private String description;

    @Column(name = "ACTIVE", nullable = false)
    private Boolean active = true;

    @OneToMany(mappedBy = "categorie", fetch = FetchType.LAZY)
    private List<Produit> produits = new ArrayList<>();
}
