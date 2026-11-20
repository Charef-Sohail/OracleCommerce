package com.enset.oraclecommerce.mapper;

import com.enset.oraclecommerce.dto.response.UtilisateurResponse;
import com.enset.oraclecommerce.entity.Utilisateur;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * @author pc
 **/
@Component
public class UtilisateurMapper {

    public UtilisateurResponse toResponse(Utilisateur utilisateur) {
        return new UtilisateurResponse(
                utilisateur.getId(),
                utilisateur.getNomUtilisateur(),
                utilisateur.getEmail(),
                utilisateur.getNom(),
                utilisateur.getPrenom(),
                utilisateur.getActif(),
                utilisateur.getDateCreation(),
                utilisateur.getRoles()
                        .stream()
                        .map(role -> role.getNom())
                        .collect(Collectors.toSet())
        );
    }
}