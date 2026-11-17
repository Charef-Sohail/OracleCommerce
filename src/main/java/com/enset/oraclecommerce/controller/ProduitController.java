package com.enset.oraclecommerce.controller;

import com.enset.oraclecommerce.dto.request.ProduitRequest;
import com.enset.oraclecommerce.dto.response.ProduitResponse;
import com.enset.oraclecommerce.service.CategorieService;
import com.enset.oraclecommerce.service.ProduitService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * @author pc
 **/
@Controller
@RequestMapping("/produits")
public class ProduitController {

    private final ProduitService produitService;
    private final CategorieService categorieService;

    public ProduitController(
            ProduitService produitService,
            CategorieService categorieService) {
        this.produitService = produitService;
        this.categorieService = categorieService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("produits", produitService.findAll());
        model.addAttribute("categories", categorieService.findAllActive());
        return "produits/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("produit",
                new ProduitRequest("", "", "", null, 0, 5, null));
        loadCategories(model);
        return "produits/form";
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("produit") ProduitRequest request,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            loadCategories(model);
            return "produits/form";
        }

        produitService.create(request);
        redirectAttributes.addFlashAttribute(
                "successMessage", "Produit ajouté avec succès.");

        return "redirect:/produits";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        ProduitResponse produit = produitService.findById(id);

        ProduitRequest request = new ProduitRequest(
                produit.reference(),
                produit.nom(),
                produit.description(),
                produit.prix(),
                produit.quantiteStock(),
                produit.seuilAlerte(),
                produit.categorieId()
        );

        model.addAttribute("produit", request);
        model.addAttribute("produitId", id);
        loadCategories(model);

        return "produits/form";
    }

    @PostMapping("/{id}/edit")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("produit") ProduitRequest request,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            model.addAttribute("produitId", id);
            loadCategories(model);
            return "produits/form";
        }

        produitService.update(id, request);
        redirectAttributes.addFlashAttribute(
                "successMessage", "Produit modifié avec succès.");

        return "redirect:/produits";
    }

    @GetMapping("/recherche")
    public String search(
            @RequestParam(defaultValue = "") String keyword,
            Model model) {

        model.addAttribute("produits", produitService.search(keyword));
        model.addAttribute("keyword", keyword);
        model.addAttribute("categories", categorieService.findAllActive());

        return "produits/list";
    }

    @GetMapping("/stock-alertes")
    public String lowStock(Model model) {
        model.addAttribute("produits", produitService.findLowStock());
        model.addAttribute("categories", categorieService.findAllActive());
        return "produits/list";
    }

    @PostMapping("/{id}/delete")
    public String delete(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        produitService.delete(id);
        redirectAttributes.addFlashAttribute(
                "successMessage", "Produit supprimé avec succès.");

        return "redirect:/produits";
    }

    private void loadCategories(Model model) {
        model.addAttribute("categories", categorieService.findAllActive());
    }
}