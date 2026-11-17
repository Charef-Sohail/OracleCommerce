package com.enset.oraclecommerce.controller;

import com.enset.oraclecommerce.dto.request.CategorieRequest;
import com.enset.oraclecommerce.dto.response.CategorieResponse;
import com.enset.oraclecommerce.service.CategorieService;
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
@RequestMapping("/categories")
public class CategorieController {

    private final CategorieService categorieService;

    public CategorieController(CategorieService categorieService) {
        this.categorieService = categorieService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("categories", categorieService.findAll());
        return "categories/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("categorie",
                new CategorieRequest("", "", true));
        return "categories/form";
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("categorie") CategorieRequest request,
            BindingResult result,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            return "categories/form";
        }

        categorieService.create(request);
        redirectAttributes.addFlashAttribute(
                "successMessage", "Catégorie ajoutée avec succès.");

        return "redirect:/categories";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        CategorieResponse categorie = categorieService.findById(id);

        CategorieRequest request = new CategorieRequest(
                categorie.nom(),
                categorie.description(),
                categorie.active()
        );

        model.addAttribute("categorie", request);
        model.addAttribute("categorieId", id);

        return "categories/form";
    }

    @PostMapping("/{id}/edit")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("categorie") CategorieRequest request,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            model.addAttribute("categorieId", id);
            return "categories/form";
        }

        categorieService.update(id, request);
        redirectAttributes.addFlashAttribute(
                "successMessage", "Catégorie modifiée avec succès.");

        return "redirect:/categories";
    }

    @PostMapping("/{id}/delete")
    public String delete(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        categorieService.delete(id);
        redirectAttributes.addFlashAttribute(
                "successMessage", "Catégorie supprimée avec succès.");

        return "redirect:/categories";
    }
}
