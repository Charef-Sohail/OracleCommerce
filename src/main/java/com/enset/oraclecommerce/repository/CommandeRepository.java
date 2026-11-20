package com.enset.oraclecommerce.repository;

import com.enset.oraclecommerce.entity.Commande;
import com.enset.oraclecommerce.entity.StatutCommande;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * @author pc
 **/
public interface CommandeRepository extends JpaRepository<Commande, Long> {

    Optional<Commande> findByNumero(String numero);

    boolean existsByNumero(String numero);

    List<Commande> findByClientId(Long clientId);

    List<Commande> findByStatut(StatutCommande statut);

    List<Commande> findByClientIdOrderByDateCommandeDesc(Long clientId);

    List<Commande> findAllByOrderByDateCommandeDesc();

    long countByStatut(StatutCommande statut);
}