package org.example.moliyaapp.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.moliyaapp.dto.ApiResponse;
import org.example.moliyaapp.dto.RoleDto;
import org.example.moliyaapp.entity.UserRole;
import org.example.moliyaapp.enums.Role;
import org.example.moliyaapp.exception.BadRequestException;
import org.example.moliyaapp.exception.ResourceNotFoundException;
import org.example.moliyaapp.mapper.RoleMapper;
import org.example.moliyaapp.repository.RoleRepository;
import org.example.moliyaapp.service.RoleService;
import org.example.moliyaapp.utils.RestConstants;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;


    @Override
    public ApiResponse addRole(RoleDto.CreateRole dto) {
        Optional<UserRole> optionalUserRole = this.roleRepository.findByNameAndDeletedFalse(dto.getName());
        if (optionalUserRole.isPresent()) {
            throw new BadRequestException(RestConstants.ROLE_ALREADY_EXISTS);
        }

        UserRole entity = this.roleMapper.toEntity(dto);
        entity = this.roleRepository.save(entity);
        RoleDto mapperDto = this.roleMapper.toDto(entity);
        return ApiResponse.builder()
                .message(RestConstants.SUCCESS)
                .data(mapperDto)
                .status(HttpStatus.OK)
                .build();

    }

    @Override
    public ApiResponse getRole(Long roleId) {
        UserRole userRole = this.roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.ROLE_NOT_FOUND));
        return ApiResponse.builder()
                .message(RestConstants.SUCCESS)
                .data(this.roleMapper.toDto(userRole))
                .status(HttpStatus.OK)
                .build();
    }

    @Override
    public ApiResponse getAllRoles(Pageable pageable) {
        Page<UserRole> rolePage = this.roleRepository.findAll(pageable);
        Set<RoleDto> collect = rolePage
                .getContent()
                .stream()
                .map(this.roleMapper::toDto)
                .collect(Collectors.toSet());
        return ApiResponse.builder()
                .message(RestConstants.SUCCESS)
                .data(collect)
                .status(HttpStatus.OK)
                .elements(rolePage.getTotalElements())
                .pages(rolePage.getTotalPages())
                .build();
    }

    @Override
    public ApiResponse getAllAllRoleList() {
        List<UserRole> userRoleList = this.roleRepository.findAll();
        List<RoleDto> roleDtos = this.roleMapper.dtoList(userRoleList);
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESS)
                .data(roleDtos)
                .build();
    }

    @Override
    public ApiResponse deleteRole(Long id) {
        UserRole userRole = this.roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.ROLE_NOT_FOUND));
        this.roleRepository.delete(userRole);
        return ApiResponse.builder()
                .message(RestConstants.SUCCESS)
                .status(HttpStatus.OK)
                .build();
    }

    @Transactional
    @Override
    public ApiResponse updateRole(Long id, RoleDto.CreateRole dto) {
        UserRole userRole = this.roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.ROLE_NOT_FOUND));
        if (userRole.getName().equals(Role.OWNER.name())) {
            throw new BadRequestException(RestConstants.OWNER_ROLE_CANNOT_BE_CREATED);
        }
        if (dto.getName().equalsIgnoreCase(Role.OWNER.name())) {
            throw new BadRequestException(RestConstants.OWNER_ROLE_CANNOT_BE_CREATED);
        }
        UserRole updatedRole = this.roleMapper.updateRole(userRole, dto);
        userRole = this.roleRepository.save(updatedRole);
        RoleDto mapperDto = this.roleMapper.toDto(userRole);
        return ApiResponse.builder()
                .message(RestConstants.SUCCESS)
                .status(HttpStatus.OK)
                .data(mapperDto)
                .build();
    }

}
