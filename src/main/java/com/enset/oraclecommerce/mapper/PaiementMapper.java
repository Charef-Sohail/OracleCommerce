package com.enset.oraclecommerce.mapper;

import com.enset.oraclecommerce.dto.response.PaiementResponse;
import com.enset.oraclecommerce.entity.Paiement;
import org.springframework.stereotype.Component;

/**
 * @author pc
 **/
@Component
public class PaiementMapper {

    public PaiementResponse toResponse(Paiement paiement) {
        return new PaiementResponse(
                paiement.getId(),
                paiement.getReferenceTransaction(),
                paiement.getMontant(),
                paiement.getModePaiement(),
                paiement.getStatut(),
                paiement.getDatePaiement(),
                paiement.getCommande().getId(),
                paiement.getCommande().getNumero()
        );
    }
}
