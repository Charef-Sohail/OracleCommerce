package com.enset.oraclecommerce.service;

import com.enset.oraclecommerce.dto.request.ProduitRequest;
import com.enset.oraclecommerce.dto.response.ProduitResponse;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author pc
 **/
public interface ProduitService {

    ProduitResponse create(ProduitRequest request);

    ProduitResponse update(Long id, ProduitRequest request);

    ProduitResponse findById(Long id);

    List<ProduitResponse> findAll();

    List<ProduitResponse> findAllActive();

    List<ProduitResponse> search(String keyword);

    List<ProduitResponse> findByCategory(Long categorieId);

    List<ProduitResponse> findByPriceRange(
            BigDecimal min,
            BigDecimal max
    );

    List<ProduitResponse> findLowStock();

    void delete(Long id);
}
