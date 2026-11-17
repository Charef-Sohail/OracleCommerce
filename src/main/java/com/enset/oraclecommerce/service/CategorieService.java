package com.enset.oraclecommerce.service;

import com.enset.oraclecommerce.dto.request.CategorieRequest;
import com.enset.oraclecommerce.dto.response.CategorieResponse;

import java.util.List;

/**
 * @author pc
 **/
public interface CategorieService {

    CategorieResponse create(CategorieRequest request);

    CategorieResponse update(Long id, CategorieRequest request);

    CategorieResponse findById(Long id);

    List<CategorieResponse> findAll();

    List<CategorieResponse> findAllActive();

    void delete(Long id);
}