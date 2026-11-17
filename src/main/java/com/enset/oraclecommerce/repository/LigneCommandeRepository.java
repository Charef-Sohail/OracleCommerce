package com.enset.oraclecommerce.repository;

import com.enset.oraclecommerce.entity.LigneCommande;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @author pc
 **/
public interface LigneCommandeRepository
        extends JpaRepository<LigneCommande, Long> {

    List<LigneCommande> findByCommandeId(Long commandeId);

    List<LigneCommande> findByProduitId(Long produitId);

    boolean existsByCommandeIdAndProduitId(
            Long commandeId,
            Long produitId
    );

    long countByCommandeId(Long commandeId);
}
