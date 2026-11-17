package com.enset.oraclecommerce.mapper;

import com.enset.oraclecommerce.dto.response.CommandeResponse;
import com.enset.oraclecommerce.entity.Commande;
import org.springframework.stereotype.Component;

/**
 * @author pc
 **/
@Component
public class CommandeMapper {

    private final LigneCommandeMapper ligneCommandeMapper;

    public CommandeMapper(LigneCommandeMapper ligneCommandeMapper) {
        this.ligneCommandeMapper = ligneCommandeMapper;
    }

    public CommandeResponse toResponse(Commande commande) {
        return new CommandeResponse(
                commande.getId(),
                commande.getNumero(),
                commande.getDateCommande(),
                commande.getStatut(),
                commande.getMontantTotal(),
                commande.getClient().getId(),
                commande.getClient().getNom() + " "
                        + commande.getClient().getPrenom(),
                commande.getLignes()
                        .stream()
                        .map(ligneCommandeMapper::toResponse)
                        .toList()
        );
    }
}