package com.enset.oraclecommerce.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * @author pc
 **/
public record LigneCommandeRequest(

        @NotNull(message = "Le produit est obligatoire")
        Long produitId,

        @NotNull(message = "La quantité est obligatoire")
        @Positive(message = "La quantité doit être supérieure à zéro")
        Integer quantite

) {
}