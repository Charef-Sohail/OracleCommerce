package com.enset.oraclecommerce.service;

import com.enset.oraclecommerce.dto.request.RoleRequest;
import com.enset.oraclecommerce.dto.response.RoleResponse;

import java.util.List;

/**
 * @author pc
 **/
public interface RoleService {

    RoleResponse create(RoleRequest request);

    RoleResponse findById(Long id);

    List<RoleResponse> findAll();

    RoleResponse update(Long id, RoleRequest request);

    void delete(Long id);
}