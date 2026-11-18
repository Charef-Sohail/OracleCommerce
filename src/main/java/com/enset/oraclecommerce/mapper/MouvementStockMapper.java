package com.enset.oraclecommerce.mapper;

import com.enset.oraclecommerce.dto.response.MouvementStockResponse;
import com.enset.oraclecommerce.entity.MouvementStock;
import org.springframework.stereotype.Component;

/**
 * @author pc
 **/
@Component
public class MouvementStockMapper {

    public MouvementStockResponse toResponse(MouvementStock mouvement) {
        return new MouvementStockResponse(
                mouvement.getId(),
                mouvement.getProduit().getId(),
                mouvement.getProduit().getReference(),
                mouvement.getProduit().getNom(),
                mouvement.getTypeMouvement(),
                mouvement.getQuantite(),
                mouvement.getQuantiteAvant(),
                mouvement.getQuantiteApres(),
                mouvement.getMotif(),
                mouvement.getDateMouvement()
        );
    }
}