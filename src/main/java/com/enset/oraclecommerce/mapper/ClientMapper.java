package com.enset.oraclecommerce.mapper;

import com.enset.oraclecommerce.dto.request.ClientRequest;
import com.enset.oraclecommerce.dto.response.ClientResponse;
import com.enset.oraclecommerce.entity.Client;
import org.springframework.stereotype.Component;

/**
 * @author pc
 **/
@Component
public class ClientMapper {

    public Client toEntity(ClientRequest request) {

        Client client = new Client();

        client.setNom(request.nom());
        client.setPrenom(request.prenom());
        client.setEmail(request.email());
        client.setTelephone(request.telephone());
        client.setAdresse(request.adresse());

        return client;
    }

    public ClientResponse toResponse(Client client) {

        return new ClientResponse(
                client.getId(),
                client.getNom(),
                client.getPrenom(),
                client.getEmail(),
                client.getTelephone(),
                client.getAdresse(),
                client.getDateCreation()
        );
    }

    public void updateEntity(Client client, ClientRequest request) {

        client.setNom(request.nom());
        client.setPrenom(request.prenom());
        client.setEmail(request.email());
        client.setTelephone(request.telephone());
        client.setAdresse(request.adresse());
    }
}