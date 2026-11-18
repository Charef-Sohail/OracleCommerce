package com.enset.oraclecommerce.service;

import com.enset.oraclecommerce.dto.request.CommandeRequest;
import com.enset.oraclecommerce.dto.response.CommandeResponse;
import com.enset.oraclecommerce.entity.StatutCommande;

import java.util.List;

/**
 * @author pc
 **/
public interface CommandeService {

    CommandeResponse create(CommandeRequest request);

    CommandeResponse findById(Long id);

    List<CommandeResponse> findAll();

    List<CommandeResponse> findByClient(Long clientId);

    List<CommandeResponse> findByStatut(StatutCommande statut);

    CommandeResponse updateStatut(Long id, StatutCommande statut);

    void cancel(Long id);
}
