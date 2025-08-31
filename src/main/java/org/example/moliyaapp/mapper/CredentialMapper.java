package org.example.moliyaapp.mapper;

import org.example.moliyaapp.dto.ChangeCredentialDto;
import org.example.moliyaapp.entity.User;
import org.springframework.stereotype.Component;

@Component
public class CredentialMapper {

    public ChangeCredentialDto toDto(User user) {
        return ChangeCredentialDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .createdBy(user.getCreatedBy())
                .updatedBy(user.getUpdatedBy())
                .deleted(user.getDeleted())
                .build();
    }

    public User toEntity(ChangeCredentialDto dto) {
        return User.builder()
                .fullName(dto.getFullName())
                .email(dto.getEmail())
                .phoneNumber(dto.getPhoneNumber())
                .build();
    }

    public void toUpdate(ChangeCredentialDto.CreateCredentialDto dto, User user) {
        if (dto.getFullName() != null && !dto.getFullName().trim().isEmpty()) {
            user.setFullName(dto.getFullName());
        }
        if (dto.getEmail() != null && !dto.getEmail().trim().isEmpty()) {
            user.setEmail(dto.getEmail());
        }
        if (dto.getPhoneNumber() != null && !dto.getPhoneNumber().trim().isEmpty()) {
            user.setPhoneNumber(dto.getPhoneNumber());
        }
    }
}
