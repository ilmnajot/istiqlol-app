package org.example.moliyaapp.mapper;

import org.example.moliyaapp.dto.RoleDto;
import org.example.moliyaapp.entity.UserRole;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RoleMapper {
    public UserRole toEntity(RoleDto.CreateRole dto) {
        return UserRole.builder()
                .name(dto.getName())
                .build();
    }

    public RoleDto toDto(UserRole role) {
        return RoleDto.builder()
                .id(role.getId())
                .name(role.getName())
                .createdBy(role.getCreatedBy())
                .updatedBy(role.getUpdatedBy())
                .createdAt(role.getCreatedAt())
                .updatedAt(role.getUpdatedAt())
                .deleted(role.getDeleted())
                .build();
    }

    public List<RoleDto> dtoList(List<UserRole> roleList) {
        if (roleList != null && !roleList.isEmpty()) {
            return roleList.stream().map(this::toDto).toList();
        }
        return new ArrayList<>();
    }

    public UserRole updateRole(UserRole userRole, RoleDto.CreateRole dto) {
        if (dto == null) return null;
        if (dto.getName() != null) {
            userRole.setName(dto.getName());
        }
        return userRole;
    }
}
