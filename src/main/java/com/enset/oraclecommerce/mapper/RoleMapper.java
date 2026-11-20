package com.enset.oraclecommerce.mapper;

import com.enset.oraclecommerce.dto.response.RoleResponse;
import com.enset.oraclecommerce.entity.Role;
import org.springframework.stereotype.Component;

/**
 * @author pc
 **/
@Component
public class RoleMapper {

    public RoleResponse toResponse(Role role) {
        return new RoleResponse(
                role.getId(),
                role.getNom(),
                role.getDescription()
        );
    }
}