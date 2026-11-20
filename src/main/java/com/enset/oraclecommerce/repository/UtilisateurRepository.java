package com.enset.oraclecommerce.repository;

import com.enset.oraclecommerce.entity.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * @author pc
 **/
public interface UtilisateurRepository
        extends JpaRepository<Utilisateur, Long> {

    Optional<Utilisateur> findByEmail(String email);

    Optional<Utilisateur> findByNomUtilisateur(String nomUtilisateur);

    boolean existsByEmail(String email);

    boolean existsByNomUtilisateur(String nomUtilisateur);

    List<Utilisateur> findByActifTrue();
}
