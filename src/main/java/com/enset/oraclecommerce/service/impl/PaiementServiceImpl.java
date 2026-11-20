package com.enset.oraclecommerce.service.impl;

import com.enset.oraclecommerce.dto.request.PaiementRequest;
import com.enset.oraclecommerce.dto.response.PaiementResponse;
import com.enset.oraclecommerce.entity.Commande;
import com.enset.oraclecommerce.entity.Paiement;
import com.enset.oraclecommerce.entity.StatutPaiement;
import com.enset.oraclecommerce.exception.BusinessException;
import com.enset.oraclecommerce.exception.ResourceNotFoundException;
import com.enset.oraclecommerce.mapper.PaiementMapper;
import com.enset.oraclecommerce.repository.CommandeRepository;
import com.enset.oraclecommerce.repository.PaiementRepository;
import com.enset.oraclecommerce.service.PaiementService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * @author pc
 **/
@Service
@Transactional
public class PaiementServiceImpl implements PaiementService {

    private final PaiementRepository paiementRepository;
    private final CommandeRepository commandeRepository;
    private final PaiementMapper paiementMapper;

    public PaiementServiceImpl(
            PaiementRepository paiementRepository,
            CommandeRepository commandeRepository,
            PaiementMapper paiementMapper) {
        this.paiementRepository = paiementRepository;
        this.commandeRepository = commandeRepository;
        this.paiementMapper = paiementMapper;
    }

    @Override
    public PaiementResponse create(PaiementRequest request) {

        Commande commande = commandeRepository.findById(request.commandeId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Commande introuvable"));

        if (paiementRepository.existsByCommandeId(request.commandeId())) {
            throw new BusinessException(
                    "Cette commande possède déjà un paiement");
        }

        if (commande.getStatut() == null
                || commande.getStatut().name().equals("ANNULEE")) {
            throw new BusinessException(
                    "Impossible de payer une commande annulée");
        }

        Paiement paiement = new Paiement();

        paiement.setReferenceTransaction(
                "PAY-" + UUID.randomUUID().toString()
                        .substring(0, 8).toUpperCase());

        paiement.setMontant(commande.getMontantTotal());
        paiement.setModePaiement(request.modePaiement());

        // Le paiement reste en attente tant qu'il n'est pas confirmé.
        paiement.setStatut(StatutPaiement.EN_ATTENTE);

        paiement.setCommande(commande);

        Paiement saved = paiementRepository.save(paiement);

        return paiementMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PaiementResponse findById(Long id) {

        Paiement paiement = paiementRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Paiement introuvable"));

        return paiementMapper.toResponse(paiement);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaiementResponse> findAll() {

        return paiementRepository.findAll()
                .stream()
                .map(paiementMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PaiementResponse findByCommande(Long commandeId) {

        Paiement paiement = paiementRepository.findByCommandeId(commandeId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Aucun paiement trouvé pour cette commande"));

        return paiementMapper.toResponse(paiement);
    }
}