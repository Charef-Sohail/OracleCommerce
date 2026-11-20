package com.enset.oraclecommerce.dto.response;

import java.math.BigDecimal;

/**
 * @author pc
 **/
public record ProduitResponse(

        Long id,
        String reference,
        String nom,
        String description,
        BigDecimal prix,
        Integer quantiteStock,
        Integer seuilAlerte,
        Boolean actif,
        Long categorieId,
        String categorieNom

) {}