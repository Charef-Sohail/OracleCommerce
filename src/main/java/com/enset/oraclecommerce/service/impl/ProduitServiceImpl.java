package com.enset.oraclecommerce.service.impl;

import com.enset.oraclecommerce.dto.request.ProduitRequest;
import com.enset.oraclecommerce.dto.response.ProduitResponse;
import com.enset.oraclecommerce.entity.Categorie;
import com.enset.oraclecommerce.entity.Produit;
import com.enset.oraclecommerce.exception.BusinessException;
import com.enset.oraclecommerce.exception.ResourceNotFoundException;
import com.enset.oraclecommerce.mapper.ProduitMapper;
import com.enset.oraclecommerce.repository.CategorieRepository;
import com.enset.oraclecommerce.repository.ProduitRepository;
import com.enset.oraclecommerce.service.ProduitService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author pc
 **/
@Service
@Transactional
public class ProduitServiceImpl implements ProduitService {

    private final ProduitRepository produitRepository;
    private final CategorieRepository categorieRepository;
    private final ProduitMapper produitMapper;

    public ProduitServiceImpl(
            ProduitRepository produitRepository,
            CategorieRepository categorieRepository,
            ProduitMapper produitMapper
    ) {
        this.produitRepository = produitRepository;
        this.categorieRepository = categorieRepository;
        this.produitMapper = produitMapper;
    }

    @Override
    public ProduitResponse create(ProduitRequest request) {

        if (produitRepository.existsByReference(request.reference())) {
            throw new BusinessException(
                    "Cette référence produit existe déjà."
            );
        }

        Categorie categorie = getCategorie(request.categorieId());

        if (!Boolean.TRUE.equals(categorie.getActive())) {
            throw new BusinessException(
                    "Impossible d'ajouter un produit à une catégorie inactive."
            );
        }

        Produit produit = produitMapper.toEntity(request, categorie);

        return produitMapper.toResponse(
                produitRepository.save(produit)
        );
    }

    @Override
    public ProduitResponse update(Long id, ProduitRequest request) {

        Produit produit = getProduit(id);

        produitRepository.findByReference(request.reference())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessException(
                            "Cette référence est déjà utilisée."
                    );
                });

        Categorie categorie = getCategorie(request.categorieId());

        if (!Boolean.TRUE.equals(categorie.getActive())) {
            throw new BusinessException(
                    "La catégorie sélectionnée est inactive."
            );
        }

        produitMapper.updateEntity(produit, request, categorie);

        return produitMapper.toResponse(
                produitRepository.save(produit)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ProduitResponse findById(Long id) {
        return produitMapper.toResponse(getProduit(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProduitResponse> findAll() {
        return produitRepository.findAll()
                .stream()
                .map(produitMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProduitResponse> findAllActive() {
        return produitRepository.findByActifTrue()
                .stream()
                .map(produitMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProduitResponse> search(String keyword) {
        return produitRepository.searchByNom(keyword)
                .stream()
                .map(produitMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProduitResponse> findByCategory(Long categorieId) {

        getCategorie(categorieId);

        return produitRepository.findByCategorieId(categorieId)
                .stream()
                .map(produitMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProduitResponse> findByPriceRange(
            BigDecimal min,
            BigDecimal max
    ) {

        if (min == null || max == null || min.compareTo(max) > 0) {
            throw new BusinessException(
                    "La fourchette de prix est invalide."
            );
        }

        return produitRepository.findByPrixBetween(min, max)
                .stream()
                .map(produitMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProduitResponse> findLowStock() {
        return produitRepository.findProduitsEnAlerteStock()
                .stream()
                .map(produitMapper::toResponse)
                .toList();
    }

    @Override
    public void delete(Long id) {
        Produit produit = getProduit(id);
        produitRepository.delete(produit);
    }

    private Produit getProduit(Long id) {
        return produitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Produit introuvable : " + id
                ));
    }

    private Categorie getCategorie(Long id) {
        return categorieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Catégorie introuvable : " + id
                ));
    }
}