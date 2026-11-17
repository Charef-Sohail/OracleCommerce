package com.enset.oraclecommerce.service;

import com.enset.oraclecommerce.dto.request.ClientRequest;
import com.enset.oraclecommerce.dto.response.ClientResponse;

import java.util.List;

/**
 * @author pc
 **/
public interface ClientService {

    ClientResponse create(ClientRequest request);

    ClientResponse update(Long id, ClientRequest request);

    ClientResponse findById(Long id);

    List<ClientResponse> findAll();

    void delete(Long id);
}
