package com.enset.oraclecommerce.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * @author pc
 **/
@Component
public class DatabaseConnectionTest implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseConnectionTest(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {

        String databaseName = jdbcTemplate.queryForObject(
                "SELECT name FROM v$database",
                String.class
        );

        System.out.println("=================================");
        System.out.println("Oracle Database connected!");
        System.out.println("Database: " + databaseName);
        System.out.println("=================================");
    }
}