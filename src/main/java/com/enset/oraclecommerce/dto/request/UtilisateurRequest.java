package com.enset.oraclecommerce.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * @author pc
 **/
public record UtilisateurRequest(

        @NotBlank(message = "Le nom d'utilisateur est obligatoire")
        @Size(max = 50)
        String nomUtilisateur,

        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "Adresse email invalide")
        @Size(max = 150)
        String email,

        @NotBlank(message = "Le mot de passe est obligatoire")
        @Size(min = 8, max = 100)
        String motDePasse,

        @Size(max = 100)
        String nom,

        @Size(max = 100)
        String prenom,

        Set<Long> rolesIds

) {
}