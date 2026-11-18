package com.enset.oraclecommerce.controller;

import com.enset.oraclecommerce.dto.request.RoleRequest;
import com.enset.oraclecommerce.dto.response.RoleResponse;
import com.enset.oraclecommerce.service.RoleService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

/**
 * @author pc
 **/
@Controller
@RequestMapping("/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    // Liste des rôles
    @GetMapping
    public String list(Model model) {
        model.addAttribute("roles", roleService.findAll());
        return "roles/list";
    }

    // Formulaire de création
    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("role", new RoleRequest(null, null));
        return "roles/form";
    }

    // Enregistrer un rôle
    @PostMapping
    public String create(
            @Valid @ModelAttribute("role") RoleRequest request,
            BindingResult result) {

        if (result.hasErrors()) {
            return "roles/form";
        }

        roleService.create(request);
        return "redirect:/roles";
    }

    // Formulaire de modification
    @GetMapping("/{id}/edit")
    public String editForm(
            @PathVariable Long id,
            Model model) {

        RoleResponse role = roleService.findById(id);

        model.addAttribute(
                "role",
                new RoleRequest(role.nom(), role.description())
        );

        model.addAttribute("roleId", id);

        return "roles/form";
    }

    // Modifier un rôle
    @PostMapping("/{id}")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("role") RoleRequest request,
            BindingResult result,
            Model model) {

        if (result.hasErrors()) {
            model.addAttribute("roleId", id);
            return "roles/form";
        }

        roleService.update(id, request);
        return "redirect:/roles";
    }

    // Supprimer un rôle
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        roleService.delete(id);
        return "redirect:/roles";
    }
}