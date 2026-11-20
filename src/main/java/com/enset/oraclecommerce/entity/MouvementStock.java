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
@Table(name = "MOUVEMENT_STOCK")
@Getter
@Setter
@NoArgsConstructor
public class MouvementStock {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "mouvement_stock_seq_generator"
    )
    @SequenceGenerator(
            name = "mouvement_stock_seq_generator",
            sequenceName = "SEQ_MOUVEMENT_STOCK",
            allocationSize = 1
    )
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "TYPE_MOUVEMENT", nullable = false, length = 30)
    private TypeMouvement typeMouvement;

    @Column(name = "QUANTITE", nullable = false)
    private Integer quantite;

    @Column(name = "QUANTITE_AVANT", nullable = false)
    private Integer quantiteAvant;

    @Column(name = "QUANTITE_APRES", nullable = false)
    private Integer quantiteApres;

    @Column(name = "MOTIF", length = 500)
    private String motif;

    @Column(name = "DATE_MOUVEMENT", nullable = false)
    private LocalDateTime dateMouvement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "PRODUIT_ID", nullable = false)
    private Produit produit;

    @PrePersist
    public void prePersist() {
        if (dateMouvement == null) {
            dateMouvement = LocalDateTime.now();
        }
    }
}
