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
@Table(
        name = "LIGNE_COMMANDE",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "UK_COMMANDE_PRODUIT",
                        columnNames = {"COMMANDE_ID", "PRODUIT_ID"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class LigneCommande {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE,
            generator = "ligne_commande_seq_generator")
    @SequenceGenerator(
            name = "ligne_commande_seq_generator",
            sequenceName = "SEQ_LIGNE_COMMANDE",
            allocationSize = 1
    )
    private Long id;

    @Column(name = "QUANTITE", nullable = false)
    private Integer quantite;

    @Column(name = "PRIX_UNITAIRE", nullable = false, precision = 12, scale = 2)
    private BigDecimal prixUnitaire;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "COMMANDE_ID", nullable = false)
    private Commande commande;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "PRODUIT_ID", nullable = false)
    private Produit produit;

    public BigDecimal getSousTotal() {
        return prixUnitaire.multiply(BigDecimal.valueOf(quantite));
    }
}