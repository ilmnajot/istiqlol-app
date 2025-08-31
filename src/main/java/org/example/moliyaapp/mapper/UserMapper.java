package org.example.moliyaapp.mapper;

import lombok.RequiredArgsConstructor;
import org.example.moliyaapp.dto.RegisterDto;
import org.example.moliyaapp.dto.UserDto;
import org.example.moliyaapp.entity.User;
import org.example.moliyaapp.entity.UserRole;
import org.example.moliyaapp.repository.RoleRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class UserMapper {
    private final RoleRepository roleRepository;

    public User toEntityByEmail(RegisterDto dto) {
        //        build.setDeleted(false);
        return User.builder()
                .email(dto.getEmail())
                .fullName(dto.getFullName())
                .build();
    }


    public User toStudent(UserDto.CreateStudent student) {
        User user = User.builder()
                .fullName(student.getFullName())
                .phoneNumber(student.getPhoneNumber())
                .build();
        user.setDeleted(false);
        return user;
    }

    public User toTeacher(UserDto.CreateEmployee teacher) {
        return User.builder()
                .fullName(teacher.getFullName())
                .phoneNumber(teacher.getPhoneNumber())
                .build();
    }

    public User toEntity(RegisterDto.RegisterEmployee dto) {
        return User.builder()
                .fullName(dto.getFullName())
                .phoneNumber(dto.getPhone())
                .email(dto.getEmail())
                .contractNumber(dto.getContractNumber())
                .build();

    }

    public UserDto toDto(User user) {
        List<UserRole> list = this.roleRepository.findByUserId(user.getId());
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .status(user.getStatus())
                .password(user.getPassword())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .roles(list != null && !list.isEmpty() ?
                        list.stream().map(UserRole::getName).toList()
                        : new ArrayList<>())
                .contractNumber(user.getContractNumber())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .createdBy(user.getCreatedBy())
                .updatedBy(user.getUpdatedBy())
                .deleted(user.getDeleted())
                .build();
    }

    public void update(User user, UserDto.UpdateUser dto) {
        if (dto == null) {
            return;
        }
        if (dto.getFullName() != null && !dto.getFullName().trim().isEmpty()) {
            user.setFullName(dto.getFullName());
        }
    }

    public List<UserDto> dtoList(List<User> list) {
        if (list != null && !list.isEmpty()) {
            return list.stream().map(this::toDto).toList();
        }
        return new ArrayList<>();
    }
}
