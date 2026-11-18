package com.enset.oraclecommerce.service.impl;

import com.enset.oraclecommerce.dto.request.RoleRequest;
import com.enset.oraclecommerce.dto.response.RoleResponse;
import com.enset.oraclecommerce.entity.Role;
import com.enset.oraclecommerce.exception.BusinessException;
import com.enset.oraclecommerce.exception.ResourceNotFoundException;
import com.enset.oraclecommerce.mapper.RoleMapper;
import com.enset.oraclecommerce.repository.RoleRepository;
import com.enset.oraclecommerce.service.RoleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author pc
 **/
@Service
@Transactional
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;

    public RoleServiceImpl(
            RoleRepository roleRepository,
            RoleMapper roleMapper) {
        this.roleRepository = roleRepository;
        this.roleMapper = roleMapper;
    }

    @Override
    public RoleResponse create(RoleRequest request) {

        if (roleRepository.existsByNom(request.nom())) {
            throw new BusinessException(
                    "Ce rôle existe déjà");
        }

        Role role = new Role();
        role.setNom(request.nom());
        role.setDescription(request.description());

        return roleMapper.toResponse(
                roleRepository.save(role));
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponse findById(Long id) {

        Role role = roleRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Rôle introuvable"));

        return roleMapper.toResponse(role);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> findAll() {

        return roleRepository.findAll()
                .stream()
                .map(roleMapper::toResponse)
                .toList();
    }

    @Override
    public RoleResponse update(Long id, RoleRequest request) {

        Role role = roleRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Rôle introuvable"));

        boolean nomChange = !role.getNom().equals(request.nom());

        if (nomChange && roleRepository.existsByNom(request.nom())) {
            throw new BusinessException(
                    "Un autre rôle possède déjà ce nom");
        }

        role.setNom(request.nom());
        role.setDescription(request.description());

        return roleMapper.toResponse(
                roleRepository.save(role));
    }

    @Override
    public void delete(Long id) {

        Role role = roleRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Rôle introuvable"));

        if (role.getUtilisateurs() != null
                && !role.getUtilisateurs().isEmpty()) {
            throw new BusinessException(
                    "Impossible de supprimer un rôle associé à des utilisateurs");
        }

        roleRepository.delete(role);
    }
}
