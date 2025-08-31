package org.example.moliyaapp.service;

import org.example.moliyaapp.dto.ApiResponse;
import org.example.moliyaapp.dto.UserDto;
import org.example.moliyaapp.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface UserService {

    ApiResponse updateUser(Long id, UserDto.UpdateUser dto);

    ApiResponse getAllActiveUser(Pageable pageable);

    ApiResponse getUserById(Long id);

    ApiResponse checkByGoogleEmail(String googleEmail);

    ApiResponse updateEmail(String email, String newEmail);

    ApiResponse checkUpdatedEmail(String oldEmail, String newEmail, String code);

    ApiResponse updatePassword(String email, String newPassword);

    ApiResponse checkUpdatedPassword(String email, String password, String code);

    ApiResponse checkUsername(String username);

    ApiResponse addRoleToUser(Long userId, Long roleId);

    ApiResponse deleteRoleFromUser(Long userId, Long roleId);

    ApiResponse getUserByToken(String token);

}
