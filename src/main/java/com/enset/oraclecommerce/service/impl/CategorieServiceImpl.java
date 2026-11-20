package com.enset.oraclecommerce.service.impl;

import com.enset.oraclecommerce.dto.request.CategorieRequest;
import com.enset.oraclecommerce.dto.response.CategorieResponse;
import com.enset.oraclecommerce.entity.Categorie;
import com.enset.oraclecommerce.exception.BusinessException;
import com.enset.oraclecommerce.exception.ResourceNotFoundException;
import com.enset.oraclecommerce.mapper.CategorieMapper;
import com.enset.oraclecommerce.repository.CategorieRepository;
import com.enset.oraclecommerce.service.CategorieService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author pc
 **/
@Service
@Transactional
public class CategorieServiceImpl implements CategorieService {

    private final CategorieRepository categorieRepository;
    private final CategorieMapper categorieMapper;

    public CategorieServiceImpl(
            CategorieRepository categorieRepository,
            CategorieMapper categorieMapper
    ) {
        this.categorieRepository = categorieRepository;
        this.categorieMapper = categorieMapper;
    }

    @Override
    public CategorieResponse create(CategorieRequest request) {

        if (categorieRepository.existsByNom(request.nom())) {
            throw new BusinessException(
                    "Cette catégorie existe déjà."
            );
        }

        Categorie categorie = categorieMapper.toEntity(request);

        return categorieMapper.toResponse(
                categorieRepository.save(categorie)
        );
    }

    @Override
    public CategorieResponse update(Long id, CategorieRequest request) {

        Categorie categorie = categorieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Catégorie introuvable : " + id
                ));

        categorieRepository.findByNom(request.nom())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessException(
                            "Une autre catégorie utilise déjà ce nom."
                    );
                });

        categorieMapper.updateEntity(categorie, request);

        return categorieMapper.toResponse(
                categorieRepository.save(categorie)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public CategorieResponse findById(Long id) {

        Categorie categorie = categorieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Catégorie introuvable : " + id
                ));

        return categorieMapper.toResponse(categorie);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategorieResponse> findAll() {

        return categorieRepository.findAll()
                .stream()
                .map(categorieMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategorieResponse> findAllActive() {

        return categorieRepository.findByActiveTrue()
                .stream()
                .map(categorieMapper::toResponse)
                .toList();
    }

    @Override
    public void delete(Long id) {

        Categorie categorie = categorieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Catégorie introuvable : " + id
                ));

        if (!categorie.getProduits().isEmpty()) {
            throw new BusinessException(
                    "Impossible de supprimer une catégorie contenant des produits."
            );
        }

        categorieRepository.delete(categorie);
    }
}
