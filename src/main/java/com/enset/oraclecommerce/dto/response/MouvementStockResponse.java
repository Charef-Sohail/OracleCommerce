package com.enset.oraclecommerce.dto.response;

import com.enset.oraclecommerce.entity.TypeMouvement;

import java.time.LocalDateTime;

/**
 * @author pc
 **/
public record MouvementStockResponse(

        Long id,
        Long produitId,
        String referenceProduit,
        String nomProduit,
        TypeMouvement typeMouvement,
        Integer quantite,
        Integer quantiteAvant,
        Integer quantiteApres,
        String motif,
        LocalDateTime dateMouvement

) {
}