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

        if (utilisateurRepository.existsByEmail(request.email())) {
            throw new BusinessException("Cet email est déjà utilisé");
        }

        if (utilisateurRepository.existsByNomUtilisateur(
                request.nomUtilisateur())) {
            throw new BusinessException(
                    "Ce nom d'utilisateur est déjà utilisé");
        }

        Utilisateur utilisateur = new Utilisateur();

        utilisateur.setNomUtilisateur(request.nomUtilisateur());
        utilisateur.setEmail(request.email());
        utilisateur.setMotDePasse(
                passwordEncoder.encode(request.motDePasse()));
        utilisateur.setNom(request.nom());
        utilisateur.setPrenom(request.prenom());
        utilisateur.setActif(true);

        utilisateur.setRoles(loadRoles(request.rolesIds()));

        Utilisateur saved = utilisateurRepository.save(utilisateur);

        return utilisateurMapper.toResponse(saved);
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
    public UtilisateurResponse update(
            Long id, UtilisateurRequest request) {

        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Utilisateur introuvable"));

        if (!utilisateur.getEmail().equals(request.email())
                && utilisateurRepository.existsByEmail(request.email())) {
            throw new BusinessException("Cet email est déjà utilisé");
        }

        if (!utilisateur.getNomUtilisateur().equals(
                request.nomUtilisateur())
                && utilisateurRepository.existsByNomUtilisateur(
                request.nomUtilisateur())) {
            throw new BusinessException(
                    "Ce nom d'utilisateur est déjà utilisé");
        }

        utilisateur.setNomUtilisateur(request.nomUtilisateur());
        utilisateur.setEmail(request.email());
        utilisateur.setNom(request.nom());
        utilisateur.setPrenom(request.prenom());

        // Le mot de passe est également renouvelé lors de cette mise à jour.
        utilisateur.setMotDePasse(
                passwordEncoder.encode(request.motDePasse()));

        utilisateur.setRoles(loadRoles(request.rolesIds()));

        return utilisateurMapper.toResponse(
                utilisateurRepository.save(utilisateur));
    }

    @Override
    public void activate(Long id) {
        Utilisateur utilisateur = findEntity(id);
        utilisateur.setActif(true);
    }

    @Override
    public void deactivate(Long id) {
        Utilisateur utilisateur = findEntity(id);
        utilisateur.setActif(false);
    }

    @Override
    public void delete(Long id) {
        Utilisateur utilisateur = findEntity(id);
        utilisateurRepository.delete(utilisateur);
    }

    private Utilisateur findEntity(Long id) {
        return utilisateurRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Utilisateur introuvable"));
    }

    private Set<Role> loadRoles(Set<Long> rolesIds) {

        if (rolesIds == null || rolesIds.isEmpty()) {
            return new HashSet<>();
        }

        List<Role> roles = roleRepository.findAllById(rolesIds);

        if (roles.size() != rolesIds.size()) {
            throw new BusinessException(
                    "Un ou plusieurs rôles sont introuvables");
        }

        return new HashSet<>(roles);
    }
}