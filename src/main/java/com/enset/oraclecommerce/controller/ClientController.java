package com.enset.oraclecommerce.controller;

import com.enset.oraclecommerce.dto.request.ClientRequest;
import com.enset.oraclecommerce.dto.response.ClientResponse;
import com.enset.oraclecommerce.service.ClientService;
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
@RequestMapping("/clients")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("clients", clientService.findAll());
        return "clients/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("client",
                new ClientRequest("", "", "", "", ""));
        return "clients/form";
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("client") ClientRequest request,
            BindingResult result,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            return "clients/form";
        }

        clientService.create(request);
        redirectAttributes.addFlashAttribute(
                "successMessage", "Client ajouté avec succès.");

        return "redirect:/clients";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        ClientResponse client = clientService.findById(id);

        ClientRequest request = new ClientRequest(
                client.nom(),
                client.prenom(),
                client.email(),
                client.telephone(),
                client.adresse()
        );

        model.addAttribute("client", request);
        model.addAttribute("clientId", id);

        return "clients/form";
    }

    @PostMapping("/{id}/edit")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("client") ClientRequest request,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            model.addAttribute("clientId", id);
            return "clients/form";
        }

        clientService.update(id, request);
        redirectAttributes.addFlashAttribute(
                "successMessage", "Client modifié avec succès.");

        return "redirect:/clients";
    }

    @PostMapping("/{id}/delete")
    public String delete(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        clientService.delete(id);
        redirectAttributes.addFlashAttribute(
                "successMessage", "Client supprimé avec succès.");

        return "redirect:/clients";
    }
}