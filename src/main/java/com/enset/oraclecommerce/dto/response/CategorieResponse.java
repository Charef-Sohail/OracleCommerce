package com.enset.oraclecommerce.dto.response;

/**
 * @author pc
 **/
public record CategorieResponse(

        Long id,
        String nom,
        String description,
        Boolean active

) {}