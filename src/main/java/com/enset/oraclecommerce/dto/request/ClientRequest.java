package com.enset.oraclecommerce.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @author pc
 **/
public record ClientRequest(

        @NotBlank(message = "Le nom est obligatoire")
        @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères")
        String nom,

        @NotBlank(message = "Le prénom est obligatoire")
        @Size(max = 100)
        String prenom,

        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "Veuillez saisir un email valide")
        @Size(max = 150)
        String email,

        @Size(max = 20)
        String telephone,

        @Size(max = 255)
        String adresse

) {}