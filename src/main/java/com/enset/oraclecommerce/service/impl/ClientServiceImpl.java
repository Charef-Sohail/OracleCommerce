package com.enset.oraclecommerce.service.impl;

import com.enset.oraclecommerce.dto.request.ClientRequest;
import com.enset.oraclecommerce.dto.response.ClientResponse;
import com.enset.oraclecommerce.entity.Client;
import com.enset.oraclecommerce.exception.BusinessException;
import com.enset.oraclecommerce.exception.ResourceNotFoundException;
import com.enset.oraclecommerce.mapper.ClientMapper;
import com.enset.oraclecommerce.repository.ClientRepository;
import com.enset.oraclecommerce.service.ClientService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author pc
 **/
@Service
@Transactional
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;

    public ClientServiceImpl(
            ClientRepository clientRepository,
            ClientMapper clientMapper
    ) {
        this.clientRepository = clientRepository;
        this.clientMapper = clientMapper;
    }

    @Override
    public ClientResponse create(ClientRequest request) {

        if (clientRepository.existsByEmail(request.email())) {
            throw new BusinessException(
                    "Cette adresse email est déjà utilisée."
            );
        }

        Client client = clientMapper.toEntity(request);

        Client savedClient = clientRepository.save(client);

        return clientMapper.toResponse(savedClient);
    }

    @Override
    public ClientResponse update(Long id, ClientRequest request) {

        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Client introuvable avec l'identifiant : " + id
                ));

        clientRepository.findByEmail(request.email())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessException(
                            "Cette adresse email est déjà utilisée."
                    );
                });

        clientMapper.updateEntity(client, request);

        Client updatedClient = clientRepository.save(client);

        return clientMapper.toResponse(updatedClient);
    }

    @Override
    @Transactional(readOnly = true)
    public ClientResponse findById(Long id) {

        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Client introuvable avec l'identifiant : " + id
                ));

        return clientMapper.toResponse(client);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClientResponse> findAll() {

        return clientRepository.findAll()
                .stream()
                .map(clientMapper::toResponse)
                .toList();
    }

    @Override
    public void delete(Long id) {

        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Client introuvable avec l'identifiant : " + id
                ));

        clientRepository.delete(client);
    }
}