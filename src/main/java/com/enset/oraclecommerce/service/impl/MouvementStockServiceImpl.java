package com.enset.oraclecommerce.service.impl;

import com.enset.oraclecommerce.dto.request.MouvementStockRequest;
import com.enset.oraclecommerce.dto.response.MouvementStockResponse;
import com.enset.oraclecommerce.entity.MouvementStock;
import com.enset.oraclecommerce.entity.Produit;
import com.enset.oraclecommerce.exception.BusinessException;
import com.enset.oraclecommerce.exception.ResourceNotFoundException;
import com.enset.oraclecommerce.mapper.MouvementStockMapper;
import com.enset.oraclecommerce.repository.MouvementStockRepository;
import com.enset.oraclecommerce.repository.ProduitRepository;
import com.enset.oraclecommerce.service.MouvementStockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author pc
 **/
@Service
@Transactional
public class MouvementStockServiceImpl implements MouvementStockService {

    private final MouvementStockRepository mouvementStockRepository;
    private final ProduitRepository produitRepository;
    private final MouvementStockMapper mouvementStockMapper;

    public MouvementStockServiceImpl(
            MouvementStockRepository mouvementStockRepository,
            ProduitRepository produitRepository,
            MouvementStockMapper mouvementStockMapper) {
        this.mouvementStockRepository = mouvementStockRepository;
        this.produitRepository = produitRepository;
        this.mouvementStockMapper = mouvementStockMapper;
    }

    @Override
    public MouvementStockResponse create(MouvementStockRequest request) {

        // Verrouiller le produit pour éviter les modifications concurrentes.
        Produit produit = produitRepository
                .findByIdForUpdate(request.produitId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Produit introuvable"));

        int quantiteAvant = produit.getQuantiteStock();
        int quantiteApres;

        switch (request.typeMouvement()) {

            case ENTREE ->
                    quantiteApres = quantiteAvant + request.quantite();

            case SORTIE -> {
                if (request.quantite() > quantiteAvant) {
                    throw new BusinessException(
                            "Stock insuffisant pour effectuer cette sortie");
                }
                quantiteApres = quantiteAvant - request.quantite();
            }

            case AJUSTEMENT ->
                // Pour un ajustement, quantite représente le nouveau stock.
                    quantiteApres = request.quantite();

            default ->
                    throw new BusinessException("Type de mouvement invalide");
        }

        // Mettre à jour le stock du produit.
        produit.setQuantiteStock(quantiteApres);
        produitRepository.save(produit);

        // Enregistrer l'historique du mouvement.
        MouvementStock mouvement = new MouvementStock();
        mouvement.setProduit(produit);
        mouvement.setTypeMouvement(request.typeMouvement());
        mouvement.setQuantite(request.quantite());
        mouvement.setQuantiteAvant(quantiteAvant);
        mouvement.setQuantiteApres(quantiteApres);
        mouvement.setMotif(request.motif());

        MouvementStock saved = mouvementStockRepository.save(mouvement);

        return mouvementStockMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public MouvementStockResponse findById(Long id) {

        MouvementStock mouvement = mouvementStockRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Mouvement de stock introuvable"));

        return mouvementStockMapper.toResponse(mouvement);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MouvementStockResponse> findAll() {

        return mouvementStockRepository.findAll()
                .stream()
                .map(mouvementStockMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MouvementStockResponse> findByProduit(Long produitId) {

        if (!produitRepository.existsById(produitId)) {
            throw new ResourceNotFoundException("Produit introuvable");
        }

        return mouvementStockRepository
                .findByProduitIdOrderByDateMouvementDesc(produitId)
                .stream()
                .map(mouvementStockMapper::toResponse)
                .toList();
    }
}