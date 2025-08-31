package org.example.moliyaapp.service;

import org.example.moliyaapp.dto.ApiResponse;
import org.example.moliyaapp.dto.RoleDto;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public interface RoleService {
    ApiResponse addRole(RoleDto.CreateRole dto);

    ApiResponse getRole(Long roleId);

    ApiResponse getAllRoles(Pageable pageable);

    ApiResponse getAllAllRoleList();

    ApiResponse deleteRole(Long id);

    ApiResponse updateRole(Long id, RoleDto.CreateRole dto);
}
