package com.enset.oraclecommerce.repository;

import com.enset.oraclecommerce.entity.MouvementStock;
import com.enset.oraclecommerce.entity.TypeMouvement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author pc
 **/
public interface MouvementStockRepository
        extends JpaRepository<MouvementStock, Long> {

    List<MouvementStock> findByProduitId(Long produitId);

    List<MouvementStock> findByTypeMouvement(TypeMouvement typeMouvement);

    List<MouvementStock> findByProduitIdOrderByDateMouvementDesc(
            Long produitId
    );

    List<MouvementStock> findByDateMouvementBetween(
            LocalDateTime debut,
            LocalDateTime fin
    );

    long countByProduitId(Long produitId);
}
