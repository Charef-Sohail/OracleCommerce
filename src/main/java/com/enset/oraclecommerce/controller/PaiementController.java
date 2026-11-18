package com.enset.oraclecommerce.controller;

import com.enset.oraclecommerce.dto.request.PaiementRequest;
import com.enset.oraclecommerce.dto.response.PaiementResponse;
import com.enset.oraclecommerce.service.CommandeService;
import com.enset.oraclecommerce.service.PaiementService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

/**
 * @author pc
 **/
@Controller
@RequestMapping("/paiements")
public class PaiementController {

    private final PaiementService paiementService;
    private final CommandeService commandeService;

    public PaiementController(
            PaiementService paiementService,
            CommandeService commandeService) {
        this.paiementService = paiementService;
        this.commandeService = commandeService;
    }

    // Liste des paiements
    @GetMapping
    public String list(Model model) {
        model.addAttribute("paiements", paiementService.findAll());
        return "paiements/list";
    }

    // Formulaire de création
    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("paiement", new PaiementRequest(null, null));
        model.addAttribute("commandes", commandeService.findAll());

        return "paiements/form";
    }

    // Enregistrer un paiement
    @PostMapping
    public String create(
            @Valid @ModelAttribute("paiement") PaiementRequest request,
            BindingResult result,
            Model model) {

        if (result.hasErrors()) {
            model.addAttribute("commandes", commandeService.findAll());
            return "paiements/form";
        }

        PaiementResponse paiement = paiementService.create(request);

        return "redirect:/paiements/" + paiement.id();
    }

    // Consulter un paiement
    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Model model) {
        model.addAttribute("paiement", paiementService.findById(id));
        return "paiements/details";
    }

    // Consulter le paiement d'une commande
    @GetMapping("/commande/{commandeId}")
    public String findByCommande(
            @PathVariable Long commandeId,
            Model model) {

        model.addAttribute(
                "paiement",
                paiementService.findByCommande(commandeId)
        );
        return "paiements/details";
    }
}