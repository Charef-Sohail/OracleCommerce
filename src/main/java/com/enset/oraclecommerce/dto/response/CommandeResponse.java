package com.enset.oraclecommerce.dto.response;

import com.enset.oraclecommerce.entity.StatutCommande;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author pc
 **/
public record CommandeResponse(
        Long id,
        String numero,
        LocalDateTime dateCommande,
        StatutCommande statut,
        BigDecimal montantTotal,
        Long clientId,
        String nomClient,
        List<LigneCommandeResponse> lignes
) {
}