package com.enset.oraclecommerce.dto.response;

import com.enset.oraclecommerce.entity.ModePaiement;
import com.enset.oraclecommerce.entity.StatutPaiement;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @author pc
 **/
public record PaiementResponse(
        Long id,
        String referenceTransaction,
        BigDecimal montant,
        ModePaiement modePaiement,
        StatutPaiement statut,
        LocalDateTime datePaiement,
        Long commandeId,
        String numeroCommande
) {
}