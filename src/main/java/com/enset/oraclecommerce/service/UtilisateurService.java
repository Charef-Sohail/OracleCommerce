package com.enset.oraclecommerce.service;

import com.enset.oraclecommerce.dto.request.UtilisateurRequest;
import com.enset.oraclecommerce.dto.response.UtilisateurResponse;

import java.util.List;

/**
 * @author pc
 **/
public interface UtilisateurService {

    UtilisateurResponse create(UtilisateurRequest request);

    UtilisateurResponse findById(Long id);

    List<UtilisateurResponse> findAll();

    UtilisateurResponse update(Long id, UtilisateurRequest request);

    void activate(Long id);

    void deactivate(Long id);

    void delete(Long id);
}