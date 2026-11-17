package com.enset.oraclecommerce.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * @author pc
 **/
public record CommandeRequest(

        @NotNull(message = "Le client est obligatoire")
        Long clientId,

        @NotEmpty(message = "Une commande doit contenir au moins un produit")
        List<@Valid LigneCommandeRequest> lignes

) {
}
