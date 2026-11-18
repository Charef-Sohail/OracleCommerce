package com.enset.oraclecommerce.service.impl;

import com.enset.oraclecommerce.dto.request.CommandeRequest;
import com.enset.oraclecommerce.dto.request.LigneCommandeRequest;
import com.enset.oraclecommerce.dto.response.CommandeResponse;
import com.enset.oraclecommerce.entity.*;
import com.enset.oraclecommerce.exception.BusinessException;
import com.enset.oraclecommerce.exception.ResourceNotFoundException;
import com.enset.oraclecommerce.mapper.CommandeMapper;
import com.enset.oraclecommerce.repository.ClientRepository;
import com.enset.oraclecommerce.repository.CommandeRepository;
import com.enset.oraclecommerce.repository.MouvementStockRepository;
import com.enset.oraclecommerce.repository.ProduitRepository;
import com.enset.oraclecommerce.service.CommandeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * @author pc
 **/

@Service
@Transactional
public class CommandeServiceImpl implements CommandeService {

    private final CommandeRepository commandeRepository;
    private final ClientRepository clientRepository;
    private final ProduitRepository produitRepository;
    private final MouvementStockRepository mouvementStockRepository;
    private final CommandeMapper commandeMapper;

    public CommandeServiceImpl(
            CommandeRepository commandeRepository,
            ClientRepository clientRepository,
            ProduitRepository produitRepository,
            MouvementStockRepository mouvementStockRepository,
            CommandeMapper commandeMapper) {

        this.commandeRepository = commandeRepository;
        this.clientRepository = clientRepository;
        this.produitRepository = produitRepository;
        this.mouvementStockRepository = mouvementStockRepository;
        this.commandeMapper = commandeMapper;
    }

    @Override
    public CommandeResponse create(CommandeRequest request) {

        Client client = clientRepository.findById(request.clientId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Client introuvable"));

        Commande commande = new Commande();
        commande.setNumero("CMD-" + UUID.randomUUID()
                .toString().substring(0, 8).toUpperCase());
        commande.setClient(client);
        commande.setStatut(StatutCommande.EN_ATTENTE);

        BigDecimal montantTotal = BigDecimal.ZERO;

        for (LigneCommandeRequest ligneRequest : request.lignes()) {

            Produit produit = produitRepository
                    .findByIdForUpdate(ligneRequest.produitId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Produit introuvable"));

            if (!Boolean.TRUE.equals(produit.getActif())) {
                throw new BusinessException(
                        "Le produit " + produit.getNom() + " est désactivé");
            }

            int quantiteDemandee = ligneRequest.quantite();

            if (produit.getQuantiteStock() < quantiteDemandee) {
                throw new BusinessException(
                        "Stock insuffisant pour le produit : "
                                + produit.getNom());
            }

            int quantiteAvant = produit.getQuantiteStock();
            int quantiteApres = quantiteAvant - quantiteDemandee;

            produit.setQuantiteStock(quantiteApres);

            LigneCommande ligne = new LigneCommande();
            ligne.setCommande(commande);
            ligne.setProduit(produit);
            ligne.setQuantite(quantiteDemandee);
            ligne.setPrixUnitaire(produit.getPrix());

            commande.getLignes().add(ligne);

            BigDecimal sousTotal = produit.getPrix()
                    .multiply(BigDecimal.valueOf(quantiteDemandee));

            montantTotal = montantTotal.add(sousTotal);

            MouvementStock mouvement = new MouvementStock();
            mouvement.setProduit(produit);
            mouvement.setTypeMouvement(TypeMouvement.SORTIE);
            mouvement.setQuantite(quantiteDemandee);
            mouvement.setQuantiteAvant(quantiteAvant);
            mouvement.setQuantiteApres(quantiteApres);
            mouvement.setMotif("Commande client");

            mouvementStockRepository.save(mouvement);
        }

        commande.setMontantTotal(montantTotal);

        Commande savedCommande = commandeRepository.save(commande);

        return commandeMapper.toResponse(savedCommande);
    }

    @Override
    @Transactional(readOnly = true)
    public CommandeResponse findById(Long id) {
        return commandeMapper.toResponse(getCommande(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommandeResponse> findAll() {
        return commandeRepository.findAllByOrderByDateCommandeDesc()
                .stream()
                .map(commandeMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommandeResponse> findByClient(Long clientId) {

        if (!clientRepository.existsById(clientId)) {
            throw new ResourceNotFoundException("Client introuvable");
        }

        return commandeRepository.findByClientIdOrderByDateCommandeDesc(clientId)
                .stream()
                .map(commandeMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommandeResponse> findByStatut(StatutCommande statut) {
        return commandeRepository.findByStatut(statut)
                .stream()
                .map(commandeMapper::toResponse)
                .toList();
    }

    @Override
    public CommandeResponse updateStatut(Long id, StatutCommande statut) {

        Commande commande = getCommande(id);

        if (commande.getStatut() == StatutCommande.ANNULEE
                || commande.getStatut() == StatutCommande.LIVREE) {
            throw new BusinessException(
                    "Impossible de modifier le statut de cette commande");
        }

        commande.setStatut(statut);

        return commandeMapper.toResponse(commande);
    }

    @Override
    public void cancel(Long id) {

        Commande commande = getCommande(id);

        if (commande.getStatut() != StatutCommande.EN_ATTENTE) {
            throw new BusinessException(
                    "Seules les commandes en attente peuvent être annulées");
        }

        for (LigneCommande ligne : commande.getLignes()) {

            Produit produit = produitRepository
                    .findByIdForUpdate(ligne.getProduit().getId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Produit introuvable"));

            int quantiteAvant = produit.getQuantiteStock();
            int quantiteApres = quantiteAvant + ligne.getQuantite();

            produit.setQuantiteStock(quantiteApres);

            MouvementStock mouvement = new MouvementStock();
            mouvement.setProduit(produit);
            mouvement.setTypeMouvement(TypeMouvement.ENTREE);
            mouvement.setQuantite(ligne.getQuantite());
            mouvement.setQuantiteAvant(quantiteAvant);
            mouvement.setQuantiteApres(quantiteApres);
            mouvement.setMotif("Annulation de la commande " + commande.getNumero());

            mouvementStockRepository.save(mouvement);
        }

        commande.setStatut(StatutCommande.ANNULEE);
    }

    private Commande getCommande(Long id) {
        return commandeRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Commande introuvable"));
    }
}