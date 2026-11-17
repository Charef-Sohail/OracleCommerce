package com.enset.oraclecommerce.mapper;

import com.enset.oraclecommerce.dto.request.CategorieRequest;
import com.enset.oraclecommerce.dto.response.CategorieResponse;
import com.enset.oraclecommerce.entity.Categorie;
import org.springframework.stereotype.Component;

/**
 * @author pc
 **/
@Component
public class CategorieMapper {

    public Categorie toEntity(CategorieRequest request) {

        Categorie categorie = new Categorie();

        categorie.setNom(request.nom());
        categorie.setDescription(request.description());

        categorie.setActive(
                request.active() != null ? request.active() : true
        );

        return categorie;
    }

    public CategorieResponse toResponse(Categorie categorie) {

        return new CategorieResponse(
                categorie.getId(),
                categorie.getNom(),
                categorie.getDescription(),
                categorie.getActive()
        );
    }

    public void updateEntity(Categorie categorie, CategorieRequest request) {

        categorie.setNom(request.nom());
        categorie.setDescription(request.description());

        if (request.active() != null) {
            categorie.setActive(request.active());
        }
    }
}