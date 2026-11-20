package com.enset.oraclecommerce.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @author pc
 **/
@Entity
@Table(name = "COMMANDE")
@Getter
@Setter
@NoArgsConstructor
public class Commande {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE,
            generator = "commande_seq_generator")
    @SequenceGenerator(
            name = "commande_seq_generator",
            sequenceName = "SEQ_COMMANDE",
            allocationSize = 1
    )
    private Long id;

    @Column(name = "NUMERO", nullable = false, unique = true, length = 50)
    private String numero;

    @Column(name = "DATE_COMMANDE", nullable = false)
    private LocalDateTime dateCommande;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUT", nullable = false, length = 30)
    private StatutCommande statut = StatutCommande.EN_ATTENTE;

    @Column(name = "MONTANT_TOTAL", nullable = false, precision = 12, scale = 2)
    private BigDecimal montantTotal = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CLIENT_ID", nullable = false)
    private Client client;

    @OneToMany(
            mappedBy = "commande",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<LigneCommande> lignes = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (dateCommande == null) {
            dateCommande = LocalDateTime.now();
        }
    }
}