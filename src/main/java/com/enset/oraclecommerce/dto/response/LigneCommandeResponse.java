package com.enset.oraclecommerce.dto.response;

import java.math.BigDecimal;

/**
 * @author pc
 **/
public record LigneCommandeResponse(
        Long id,
        Long produitId,
        String referenceProduit,
        String nomProduit,
        Integer quantite,
        BigDecimal prixUnitaire,
        BigDecimal sousTotal
) {
}