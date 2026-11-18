package com.enset.oraclecommerce.service.impl;

import com.enset.oraclecommerce.dto.request.UtilisateurRequest;
import com.enset.oraclecommerce.dto.response.UtilisateurResponse;
import com.enset.oraclecommerce.entity.Role;
import com.enset.oraclecommerce.entity.Utilisateur;
import com.enset.oraclecommerce.exception.BusinessException;
import com.enset.oraclecommerce.exception.ResourceNotFoundException;
import com.enset.oraclecommerce.mapper.UtilisateurMapper;
import com.enset.oraclecommerce.repository.RoleRepository;
import com.enset.oraclecommerce.repository.UtilisateurRepository;
import com.enset.oraclecommerce.service.UtilisateurService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author pc
 **/
@Service
@Transactional
public class UtilisateurServiceImpl implements UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final RoleRepository roleRepository;
    private final UtilisateurMapper utilisateurMapper;
    private final PasswordEncoder passwordEncoder;

    public UtilisateurServiceImpl(
            UtilisateurRepository utilisateurRepository,
            RoleRepository roleRepository,
            UtilisateurMapper utilisateurMapper,
            PasswordEncoder passwordEncoder) {
        this.utilisateurRepository = utilisateurRepository;
        this.roleRepository = roleRepository;
        this.utilisateurMapper = utilisateurMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UtilisateurResponse create(UtilisateurRequest request) {

        if (utilisateurRepository.existsByNomUtilisateur(
                request.nomUtilisateur())) {
            throw new BusinessException(
                    "Ce nom d'utilisateur est déjà utilisé");
        }

        if (utilisateurRepository.existsByEmail(request.email())) {
            throw new BusinessException(
                    "Cette adresse email est déjà utilisée");
        }

        Set<Role> roles = loadRoles(request.rolesIds());

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setNomUtilisateur(request.nomUtilisateur());
        utilisateur.setEmail(request.email());
        utilisateur.setMotDePasse(
                passwordEncoder.encode(request.motDePasse()));
        utilisateur.setNom(request.nom());
        utilisateur.setPrenom(request.prenom());
        utilisateur.setActif(true);
        utilisateur.setRoles(roles);

        return utilisateurMapper.toResponse(
                utilisateurRepository.save(utilisateur));
    }

    @Override
    @Transactional(readOnly = true)
    public UtilisateurResponse findById(Long id) {

        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Utilisateur introuvable"));

        return utilisateurMapper.toResponse(utilisateur);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UtilisateurResponse> findAll() {

        return utilisateurRepository.findAll()
                .stream()
                .map(utilisateurMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UtilisateurResponse> findAllActive() {

        return utilisateurRepository.findByActifTrue()
                .stream()
                .map(utilisateurMapper::toResponse)
                .toList();
    }

    @Override
    public UtilisateurResponse update(
            Long id, UtilisateurRequest request) {

        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Utilisateur introuvable"));

        utilisateurRepository.findByNomUtilisateur(
                request.nomUtilisateur()
        ).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new BusinessException(
                        "Ce nom d'utilisateur est déjà utilisé");
            }
        });

        utilisateurRepository.findByEmail(request.email())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new BusinessException(
                                "Cette adresse email est déjà utilisée");
                    }
                });

        utilisateur.setNomUtilisateur(request.nomUtilisateur());
        utilisateur.setEmail(request.email());
        utilisateur.setNom(request.nom());
        utilisateur.setPrenom(request.prenom());

        // Ne modifier le mot de passe que s'il est renseigné.
        if (request.motDePasse() != null
                && !request.motDePasse().isBlank()) {
            utilisateur.setMotDePasse(
                    passwordEncoder.encode(request.motDePasse()));
        }

        utilisateur.setRoles(loadRoles(request.rolesIds()));

        return utilisateurMapper.toResponse(
                utilisateurRepository.save(utilisateur));
    }

    @Override
    public void delete(Long id) {

        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Utilisateur introuvable"));

        utilisateurRepository.delete(utilisateur);
    }

    private Set<Role> loadRoles(Set<Long> rolesIds) {

        if (rolesIds == null || rolesIds.isEmpty()) {
            throw new BusinessException(
                    "Un utilisateur doit posséder au moins un rôle");
        }

        List<Role> roles = roleRepository.findAllById(rolesIds);

        if (roles.size() != rolesIds.size()) {
            throw new BusinessException(
                    "Un ou plusieurs rôles sélectionnés n'existent pas");
        }

        return new HashSet<>(roles);
    }
}