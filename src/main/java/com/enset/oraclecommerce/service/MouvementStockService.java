package com.enset.oraclecommerce.service;

import com.enset.oraclecommerce.dto.request.MouvementStockRequest;
import com.enset.oraclecommerce.dto.response.MouvementStockResponse;

import java.util.List;

/**
 * @author pc
 **/
public interface MouvementStockService {

    MouvementStockResponse create(MouvementStockRequest request);

    MouvementStockResponse findById(Long id);

    List<MouvementStockResponse> findAll();

    List<MouvementStockResponse> findByProduit(Long produitId);
}