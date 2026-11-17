package com.enset.oraclecommerce.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * @author pc
 **/
public record ProduitRequest(

        @NotBlank(message = "La référence est obligatoire")
        @Size(max = 50)
        String reference,

        @NotBlank(message = "Le nom du produit est obligatoire")
        @Size(max = 150)
        String nom,

        @Size(max = 1000)
        String description,

        @NotNull(message = "Le prix est obligatoire")
        @DecimalMin(value = "0.01", message = "Le prix doit être supérieur à zéro")
        @Digits(integer = 10, fraction = 2)
        BigDecimal prix,

        @NotNull(message = "La quantité est obligatoire")
        @Min(value = 0, message = "La quantité ne peut pas être négative")
        Integer quantiteStock,

        @NotNull(message = "Le seuil d'alerte est obligatoire")
        @Min(value = 0)
        Integer seuilAlerte,

        @NotNull(message = "La catégorie est obligatoire")
        Long categorieId

) {}