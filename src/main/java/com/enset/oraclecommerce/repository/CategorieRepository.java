package com.enset.oraclecommerce.repository;

import com.enset.oraclecommerce.entity.Categorie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @author pc
 **/
@Repository
public interface CategorieRepository extends JpaRepository<Categorie, Long> {

    Optional<Categorie> findByNom(String nom);

    boolean existsByNom(String nom);

    List<Categorie> findByActiveTrue();
}