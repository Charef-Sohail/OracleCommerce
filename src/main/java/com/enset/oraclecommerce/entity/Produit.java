package com.enset.oraclecommerce.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * @author pc
 **/
@Entity
@Table(name = "PRODUIT")
@Getter
@Setter
@NoArgsConstructor
public class Produit {
    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "produit_seq_generator"
    )
    @SequenceGenerator(
            name = "produit_seq_generator",
            sequenceName = "SEQ_PRODUIT",
            allocationSize = 1
    )
    private Long id;

    @Column(name = "REFERENCE", nullable = false, unique = true, length = 50)
    private String reference;

    @Column(name = "NOM", nullable = false, length = 150)
    private String nom;

    @Column(name = "DESCRIPTION", length = 1000)
    private String description;

    @Column(name = "PRIX", nullable = false, precision = 12, scale = 2)
    private BigDecimal prix;

    @Column(name = "QUANTITE_STOCK", nullable = false)
    private Integer quantiteStock = 0;

    @Column(name = "SEUIL_ALERTE", nullable = false)
    private Integer seuilAlerte = 5;

    @Column(name = "ACTIF", nullable = false)
    private Boolean actif = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CATEGORIE_ID", nullable = false)
    private Categorie categorie;

}
