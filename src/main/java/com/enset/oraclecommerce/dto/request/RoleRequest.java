package com.enset.oraclecommerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @author pc
 **/
public record RoleRequest(

        @NotBlank(message = "Le nom du rôle est obligatoire")
        @Size(max = 50)
        String nom,

        @Size(max = 255)
        String description

) {
}