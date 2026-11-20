package com.enset.oraclecommerce.repository;

import com.enset.oraclecommerce.entity.Paiement;
import com.enset.oraclecommerce.entity.StatutPaiement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * @author pc
 **/
public interface PaiementRepository extends JpaRepository<Paiement, Long> {

    Optional<Paiement> findByReferenceTransaction(String referenceTransaction);

    Optional<Paiement> findByCommandeId(Long commandeId);

    boolean existsByCommandeId(Long commandeId);

    List<Paiement> findByStatut(StatutPaiement statut);
}