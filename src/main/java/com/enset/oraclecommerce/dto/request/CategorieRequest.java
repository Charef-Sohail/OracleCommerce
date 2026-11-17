package com.enset.oraclecommerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @author pc
 **/
public record CategorieRequest(

        @NotBlank(message = "Le nom de la catégorie est obligatoire")
        @Size(max = 100)
        String nom,

        @Size(max = 500)
        String description,

        Boolean active

) {}