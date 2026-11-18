package com.enset.oraclecommerce.controller;

import com.enset.oraclecommerce.dto.request.UtilisateurRequest;
import com.enset.oraclecommerce.dto.response.UtilisateurResponse;
import com.enset.oraclecommerce.service.RoleService;
import com.enset.oraclecommerce.service.UtilisateurService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

/**
 * @author pc
 **/
@Controller
@RequestMapping("/utilisateurs")
public class UtilisateurController {

    private final UtilisateurService utilisateurService;
    private final RoleService roleService;

    public UtilisateurController(
            UtilisateurService utilisateurService,
            RoleService roleService) {
        this.utilisateurService = utilisateurService;
        this.roleService = roleService;
    }

    // Liste des utilisateurs
    @GetMapping
    public String list(Model model) {
        model.addAttribute(
                "utilisateurs",
                utilisateurService.findAll()
        );

        return "utilisateurs/list";
    }

    // Formulaire de création
    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute(
                "utilisateur",
                new UtilisateurRequest(
                        null, null, null, null, null, null)
        );

        loadFormData(model);
        return "utilisateurs/form";
    }

    // Enregistrer un utilisateur
    @PostMapping
    public String create(
            @Valid @ModelAttribute("utilisateur")
            UtilisateurRequest request,
            BindingResult result,
            Model model) {

        if (result.hasErrors()) {
            loadFormData(model);
            return "utilisateurs/form";
        }

        utilisateurService.create(request);

        return "redirect:/utilisateurs";
    }

    // Formulaire de modification
    @GetMapping("/{id}/edit")
    public String editForm(
            @PathVariable Long id,
            Model model) {

        UtilisateurResponse utilisateur =
                utilisateurService.findById(id);

        model.addAttribute(
                "utilisateur",
                new UtilisateurRequest(
                        utilisateur.nomUtilisateur(),
                        utilisateur.email(),
                        null,
                        utilisateur.nom(),
                        utilisateur.prenom(),
                        null
                )
        );

        model.addAttribute("utilisateurId", id);
        loadFormData(model);

        return "utilisateurs/form";
    }

    // Modifier un utilisateur
    @PostMapping("/{id}")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("utilisateur")
            UtilisateurRequest request,
            BindingResult result,
            Model model) {

        if (result.hasErrors()) {
            model.addAttribute("utilisateurId", id);
            loadFormData(model);
            return "utilisateurs/form";
        }

        utilisateurService.update(id, request);

        return "redirect:/utilisateurs";
    }

    // Activer un compte
    @PostMapping("/{id}/activer")
    public String activate(@PathVariable Long id) {
        utilisateurService.activate(id);
        return "redirect:/utilisateurs";
    }

    // Désactiver un compte
    @PostMapping("/{id}/desactiver")
    public String deactivate(@PathVariable Long id) {
        utilisateurService.deactivate(id);
        return "redirect:/utilisateurs";
    }

    // Supprimer un compte
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        utilisateurService.delete(id);
        return "redirect:/utilisateurs";
    }

    private void loadFormData(Model model) {
        model.addAttribute("roles", roleService.findAll());
    }
}