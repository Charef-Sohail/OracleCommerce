package com.enset.oraclecommerce.controller;

import com.enset.oraclecommerce.dto.request.MouvementStockRequest;
import com.enset.oraclecommerce.dto.response.MouvementStockResponse;
import com.enset.oraclecommerce.service.MouvementStockService;
import com.enset.oraclecommerce.service.ProduitService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

/**
 * @author pc
 **/
@Controller
@RequestMapping("/mouvements-stock")
public class MouvementStockController {

    private final MouvementStockService mouvementStockService;
    private final ProduitService produitService;

    public MouvementStockController(
            MouvementStockService mouvementStockService,
            ProduitService produitService) {
        this.mouvementStockService = mouvementStockService;
        this.produitService = produitService;
    }

    // Liste de tous les mouvements
    @GetMapping
    public String list(Model model) {
        model.addAttribute(
                "mouvements",
                mouvementStockService.findAll()
        );

        return "mouvements-stock/list";
    }

    // Formulaire de création
    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute(
                "mouvement",
                new MouvementStockRequest(null, null, null, null)
        );

        loadFormData(model);
        return "mouvements-stock/form";
    }

    // Enregistrer un mouvement
    @PostMapping
    public String create(
            @Valid @ModelAttribute("mouvement")
            MouvementStockRequest request,
            BindingResult result,
            Model model) {

        if (result.hasErrors()) {
            loadFormData(model);
            return "mouvements-stock/form";
        }

        MouvementStockResponse mouvement =
                mouvementStockService.create(request);

        return "redirect:/mouvements-stock/"
                + mouvement.id();
    }

    // Consulter un mouvement
    @GetMapping("/{id}")
    public String details(
            @PathVariable Long id,
            Model model) {

        model.addAttribute(
                "mouvement",
                mouvementStockService.findById(id)
        );

        return "mouvements-stock/details";
    }

    // Historique des mouvements d'un produit
    @GetMapping("/produit/{produitId}")
    public String historiqueProduit(
            @PathVariable Long produitId,
            Model model) {

        model.addAttribute(
                "mouvements",
                mouvementStockService.findByProduit(produitId)
        );

        model.addAttribute(
                "produit",
                produitService.findById(produitId)
        );

        return "mouvements-stock/list";
    }

    private void loadFormData(Model model) {
        model.addAttribute(
                "produits",
                produitService.findAllActive()
        );
    }
}