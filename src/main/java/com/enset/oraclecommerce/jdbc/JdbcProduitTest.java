package com.enset.oraclecommerce.jdbc;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author pc
 **/
@Component
public class JdbcProduitTest implements CommandLineRunner {

    private final JdbcProduitRepository jdbcProduitRepository;

    public JdbcProduitTest(JdbcProduitRepository jdbcProduitRepository) {
        this.jdbcProduitRepository = jdbcProduitRepository;
    }

    @Override
    public void run(String... args) {

        System.out.println("\n========== TEST JDBC ==========");

        List<JdbcProduitStock> produits =
                jdbcProduitRepository.findProduitsEnAlerteStock();

        if (produits.isEmpty()) {
            System.out.println("Aucun produit en alerte.");
        } else {
            for (JdbcProduitStock produit : produits) {
                System.out.println("------------------------------");
                System.out.println("ID       : " + produit.id());
                System.out.println("Référence: " + produit.reference());
                System.out.println("Nom      : " + produit.nom());
                System.out.println("Prix     : " + produit.prix());
                System.out.println("Stock    : " + produit.quantiteStock());
                System.out.println("Seuil    : " + produit.seuilAlerte());
            }
        }

        System.out.println("Nombre de produits en alerte : "
                + produits.size());

        System.out.println("========== FIN TEST JDBC ==========\n");
    }
}