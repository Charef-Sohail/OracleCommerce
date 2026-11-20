package com.enset.oraclecommerce.repository;

import com.enset.oraclecommerce.entity.Produit;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * @author pc
 **/
@Repository
public interface ProduitRepository extends JpaRepository<Produit, Long> {

    Optional<Produit> findByReference(String reference);

    boolean existsByReference(String reference);

    List<Produit> findByActifTrue();

    List<Produit> findByCategorieId(Long categorieId);

    List<Produit> findByPrixBetween(
            BigDecimal prixMin,
            BigDecimal prixMax
    );

    @Query("""
           SELECT p FROM Produit p
           WHERE LOWER(p.nom) LIKE LOWER(CONCAT('%', :keyword, '%'))
           """)
    List<Produit> searchByNom(@Param("keyword") String keyword);

    @Query("""
           SELECT p FROM Produit p
           WHERE p.quantiteStock <= p.seuilAlerte
           """)
    List<Produit> findProduitsEnAlerteStock();


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Produit p WHERE p.id = :id")
    Optional<Produit> findByIdForUpdate(@Param("id") Long id);
}