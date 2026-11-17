package com.enset.oraclecommerce.mapper;

import com.enset.oraclecommerce.dto.request.ProduitRequest;
import com.enset.oraclecommerce.dto.response.ProduitResponse;
import com.enset.oraclecommerce.entity.Categorie;
import com.enset.oraclecommerce.entity.Produit;
import org.springframework.stereotype.Component;

/**
 * @author pc
 **/
@Component
public class ProduitMapper {

    public Produit toEntity(
            ProduitRequest request,
            Categorie categorie
    ) {

        Produit produit = new Produit();

        produit.setReference(request.reference());
        produit.setNom(request.nom());
        produit.setDescription(request.description());
        produit.setPrix(request.prix());
        produit.setQuantiteStock(request.quantiteStock());
        produit.setSeuilAlerte(request.seuilAlerte());
        produit.setActif(true);
        produit.setCategorie(categorie);

        return produit;
    }

    public ProduitResponse toResponse(Produit produit) {

        return new ProduitResponse(
                produit.getId(),
                produit.getReference(),
                produit.getNom(),
                produit.getDescription(),
                produit.getPrix(),
                produit.getQuantiteStock(),
                produit.getSeuilAlerte(),
                produit.getActif(),
                produit.getCategorie().getId(),
                produit.getCategorie().getNom()
        );
    }

    public void updateEntity(
            Produit produit,
            ProduitRequest request,
            Categorie categorie
    ) {

        produit.setReference(request.reference());
        produit.setNom(request.nom());
        produit.setDescription(request.description());
        produit.setPrix(request.prix());
        produit.setQuantiteStock(request.quantiteStock());
        produit.setSeuilAlerte(request.seuilAlerte());
        produit.setCategorie(categorie);
    }
}