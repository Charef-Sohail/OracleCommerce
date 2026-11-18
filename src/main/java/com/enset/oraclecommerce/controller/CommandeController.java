package com.enset.oraclecommerce.controller;

import com.enset.oraclecommerce.dto.request.CommandeRequest;
import com.enset.oraclecommerce.dto.response.CommandeResponse;
import com.enset.oraclecommerce.entity.StatutCommande;
import com.enset.oraclecommerce.service.ClientService;
import com.enset.oraclecommerce.service.CommandeService;
import com.enset.oraclecommerce.service.ProduitService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author pc
 **/
@Controller
@RequestMapping("/commandes")
public class CommandeController {

    private final CommandeService commandeService;
    private final ClientService clientService;
    private final ProduitService produitService;

    public CommandeController(
            CommandeService commandeService,
            ClientService clientService,
            ProduitService produitService) {
        this.commandeService = commandeService;
        this.clientService = clientService;
        this.produitService = produitService;
    }

    // Liste des commandes
    @GetMapping
    public String list(Model model) {
        model.addAttribute("commandes", commandeService.findAll());
        return "commandes/list";
    }

    // Formulaire de création
    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("commande",
                new CommandeRequest(null, List.of()));

        loadFormData(model);
        return "commandes/form";
    }

    // Enregistrement d'une commande
    @PostMapping
    public String create(
            @Valid @ModelAttribute("commande") CommandeRequest request,
            BindingResult result,
            Model model) {

        if (result.hasErrors()) {
            loadFormData(model);
            return "commandes/form";
        }

        commandeService.create(request);
        return "redirect:/commandes";
    }

    // Détails d'une commande
    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Model model) {
        CommandeResponse commande = commandeService.findById(id);

        model.addAttribute("commande", commande);
        model.addAttribute("statuts", StatutCommande.values());

        return "commandes/details";
    }

    // Modification du statut
    @PostMapping("/{id}/statut")
    public String updateStatut(
            @PathVariable Long id,
            @RequestParam StatutCommande statut) {

        commandeService.updateStatut(id, statut);
        return "redirect:/commandes/" + id;
    }

    // Annulation d'une commande
    @PostMapping("/{id}/annuler")
    public String cancel(@PathVariable Long id) {
        commandeService.cancel(id);
        return "redirect:/commandes";
    }

    private void loadFormData(Model model) {
        model.addAttribute("clients", clientService.findAll());
        model.addAttribute("produits", produitService.findAllActive());
    }
}