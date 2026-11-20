package com.enset.oraclecommerce.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @author pc
 **/
@Entity
@Table(name = "PAIEMENT")
@Getter
@Setter
@NoArgsConstructor
public class Paiement {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "paiement_seq_generator"
    )
    @SequenceGenerator(
            name = "paiement_seq_generator",
            sequenceName = "SEQ_PAIEMENT",
            allocationSize = 1
    )
    private Long id;

    @Column(name = "REFERENCE_TRANSACTION", unique = true, length = 100)
    private String referenceTransaction;

    @Column(name = "MONTANT", nullable = false, precision = 12, scale = 2)
    private BigDecimal montant;

    @Enumerated(EnumType.STRING)
    @Column(name = "MODE_PAIEMENT", nullable = false, length = 30)
    private ModePaiement modePaiement;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUT", nullable = false, length = 30)
    private StatutPaiement statut = StatutPaiement.EN_ATTENTE;

    @Column(name = "DATE_PAIEMENT", nullable = false)
    private LocalDateTime datePaiement;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "COMMANDE_ID", nullable = false, unique = true)
    private Commande commande;

    @PrePersist
    public void prePersist() {
        if (datePaiement == null) {
            datePaiement = LocalDateTime.now();
        }
    }
}