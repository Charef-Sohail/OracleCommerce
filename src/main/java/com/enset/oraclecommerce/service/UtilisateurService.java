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

    List<UtilisateurResponse> findAllActive();

    UtilisateurResponse update(Long id, UtilisateurRequest request);

    void delete(Long id);
}
