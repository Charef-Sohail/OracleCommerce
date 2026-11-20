package com.enset.oraclecommerce.dto.request;

import com.enset.oraclecommerce.entity.TypeMouvement;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * @author pc
 **/
public record MouvementStockRequest(

        @NotNull(message = "Le produit est obligatoire")
        Long produitId,

        @NotNull(message = "Le type de mouvement est obligatoire")
        TypeMouvement typeMouvement,

        @NotNull(message = "La quantité est obligatoire")
        @Positive(message = "La quantité doit être supérieure à zéro")
        Integer quantite,

        @Size(max = 500, message = "Le motif ne doit pas dépasser 500 caractères")
        String motif

) {
}