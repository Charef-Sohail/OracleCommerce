package com.enset.oraclecommerce.dto.response;

import java.math.BigDecimal;

/**
 * @author pc
 **/
public record DashboardStats(
        Long totalClients,
        Long totalProduits,
        Long totalCategories,
        Long produitsEnAlerte,
        BigDecimal valeurTotaleStock
) {
}