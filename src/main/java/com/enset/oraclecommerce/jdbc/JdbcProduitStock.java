package com.enset.oraclecommerce.jdbc;

import java.math.BigDecimal;

/**
 * @author pc
 **/
public record JdbcProduitStock(
        Long id,
        String reference,
        String nom,
        BigDecimal prix,
        Integer quantiteStock,
        Integer seuilAlerte
) {
}