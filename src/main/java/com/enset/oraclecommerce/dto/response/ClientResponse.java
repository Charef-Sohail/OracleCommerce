package com.enset.oraclecommerce.dto.response;

import java.time.LocalDateTime;

/**
 * @author pc
 **/
public record ClientResponse(

        Long id,
        String nom,
        String prenom,
        String email,
        String telephone,
        String adresse,
        LocalDateTime dateCreation

) {}