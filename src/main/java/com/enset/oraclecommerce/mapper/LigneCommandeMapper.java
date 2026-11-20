package com.enset.oraclecommerce.mapper;

import com.enset.oraclecommerce.dto.response.LigneCommandeResponse;
import com.enset.oraclecommerce.entity.LigneCommande;
import org.springframework.stereotype.Component;

/**
 * @author pc
 **/
@Component
public class LigneCommandeMapper {

    public LigneCommandeResponse toResponse(LigneCommande ligne) {
        return new LigneCommandeResponse(
                ligne.getId(),
                ligne.getProduit().getId(),
                ligne.getProduit().getReference(),
                ligne.getProduit().getNom(),
                ligne.getQuantite(),
                ligne.getPrixUnitaire(),
                ligne.getSousTotal()
        );
    }
}