package com.enset.oraclecommerce.jdbc;

import com.enset.oraclecommerce.dto.response.DashboardStats;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author pc
 **/
@Repository
public class JdbcDashboardRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcDashboardRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public DashboardStats getDashboardStats() {

        String sql = """
            SELECT
                (SELECT COUNT(*) FROM CLIENT) AS TOTAL_CLIENTS,
                (SELECT COUNT(*) FROM PRODUIT) AS TOTAL_PRODUITS,
                (SELECT COUNT(*) FROM CATEGORIE) AS TOTAL_CATEGORIES,
                (SELECT COUNT(*) FROM PRODUIT
                    WHERE QUANTITE_STOCK <= SEUIL_ALERTE
                      AND ACTIF = 1) AS PRODUITS_EN_ALERTE,
                (SELECT NVL(SUM(PRIX * QUANTITE_STOCK), 0)
                    FROM PRODUIT
                    WHERE ACTIF = 1) AS VALEUR_TOTALE_STOCK
            FROM DUAL
            """;

        return jdbcTemplate.queryForObject(
                sql,
                (rs, rowNum) -> new DashboardStats(
                        rs.getLong("TOTAL_CLIENTS"),
                        rs.getLong("TOTAL_PRODUITS"),
                        rs.getLong("TOTAL_CATEGORIES"),
                        rs.getLong("PRODUITS_EN_ALERTE"),
                        rs.getBigDecimal("VALEUR_TOTALE_STOCK")
                )
        );
    }
}
