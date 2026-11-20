package com.enset.oraclecommerce.dto.response;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * @author pc
 **/
public record UtilisateurResponse(

        Long id,
        String nomUtilisateur,
        String email,
        String nom,
        String prenom,
        Boolean actif,
        LocalDateTime dateCreation,
        Set<String> roles

) {
}