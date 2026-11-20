package com.enset.oraclecommerce.service;

import com.enset.oraclecommerce.dto.request.PaiementRequest;
import com.enset.oraclecommerce.dto.response.PaiementResponse;

import java.util.List;

/**
 * @author pc
 **/
public interface PaiementService {

    PaiementResponse create(PaiementRequest request);

    PaiementResponse findById(Long id);

    List<PaiementResponse> findAll();

    PaiementResponse findByCommande(Long commandeId);
}