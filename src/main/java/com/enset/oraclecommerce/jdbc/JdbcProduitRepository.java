package com.enset.oraclecommerce.jdbc;

import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author pc
 **/
@Repository
public class JdbcProduitRepository {

    private final DataSource dataSource;

    public JdbcProduitRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<JdbcProduitStock> findProduitsEnAlerteStock() {

        String sql = """
                SELECT ID, REFERENCE, NOM, PRIX,
                       QUANTITE_STOCK, SEUIL_ALERTE
                FROM PRODUIT
                WHERE QUANTITE_STOCK <= SEUIL_ALERTE
                ORDER BY QUANTITE_STOCK ASC
                """;

        List<JdbcProduitStock> produits = new ArrayList<>();

        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()
        ) {

            while (resultSet.next()) {

                JdbcProduitStock produit = new JdbcProduitStock(
                        resultSet.getLong("ID"),
                        resultSet.getString("REFERENCE"),
                        resultSet.getString("NOM"),
                        resultSet.getBigDecimal("PRIX"),
                        resultSet.getInt("QUANTITE_STOCK"),
                        resultSet.getInt("SEUIL_ALERTE")
                );

                produits.add(produit);
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Erreur lors de la récupération des produits", e
            );
        }

        return produits;
    }
}