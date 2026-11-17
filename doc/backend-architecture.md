# OracleCommerce - Backend Architecture

## Architecture
Monolithic application based on Spring Boot.

## Technologies
- Java 21
- Spring Boot 4.1.2
- Spring Data JPA
- JDBC
- Oracle Database 19c
- Thymeleaf

## Layers

- Controller: handles HTTP requests and Thymeleaf views.
- Service: contains business logic.
- Repository: handles database access using JPA.
- JDBC: executes Oracle SQL and PL/SQL operations.
- Entity: represents database tables.
- DTO: transfers data between application layers.
- Mapper: converts entities into DTOs and vice versa.
- Exception: handles application errors.
- Config: contains application configurations.