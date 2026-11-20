package com.enset.oraclecommerce.dto.request;

import com.enset.oraclecommerce.entity.ModePaiement;
import jakarta.validation.constraints.NotNull;

/**
 * @author pc
 **/
public record PaiementRequest(

        @NotNull(message = "La commande est obligatoire")
        Long commandeId,

        @NotNull(message = "Le mode de paiement est obligatoire")
        ModePaiement modePaiement

) {
}