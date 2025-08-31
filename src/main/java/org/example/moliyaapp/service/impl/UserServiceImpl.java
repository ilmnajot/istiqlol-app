package org.example.moliyaapp.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.moliyaapp.dto.ApiResponse;
import org.example.moliyaapp.dto.UserDto;
import org.example.moliyaapp.entity.Code;
import org.example.moliyaapp.entity.User;
import org.example.moliyaapp.entity.UserRole;
import org.example.moliyaapp.exception.ResourceNotFoundException;
import org.example.moliyaapp.mapper.UserMapper;
import org.example.moliyaapp.repository.CodeRepository;
import org.example.moliyaapp.repository.RoleRepository;
import org.example.moliyaapp.repository.UserRepository;
import org.example.moliyaapp.service.UserService;
import org.example.moliyaapp.utils.JwtUtil;
import org.example.moliyaapp.utils.RestConstants;
import org.example.moliyaapp.utils.Utils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final CodeRepository codeRepository;
    private final RoleRepository roleRepository;
    private final JwtUtil jwtUtil;
    private final Utils utils;
    private final UserDetailsService userDetailsService;


    @Override
    public ApiResponse updateUser(Long id, UserDto.UpdateUser dto) {
        User user = this.userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.USER_NOT_FOUND));
        this.userMapper.update(user, dto);
        user.setUpdatedAt(LocalDateTime.now());
        this.userRepository.save(user);
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message("User updated")
                .build();
    }

    @Override
    public ApiResponse getAllActiveUser(Pageable pageable) {
        Page<User> page = this.userRepository.getAllActiveUsers(pageable);
        if (page != null && !page.isEmpty()) {
            return ApiResponse.builder()
                    .status(HttpStatus.OK)
                    .data(this.userMapper.dtoList(page.getContent()))
                    .elements(page.getTotalElements())
                    .pages(page.getTotalPages())
                    .build();
        }
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .data(new ArrayList<>())
                .build();
    }

    @Override
    public ApiResponse getUserById(Long id) {
        User user = this.userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.USER_NOT_FOUND));

        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .data(this.userMapper.toDto(user))
                .build();
    }

    @Override
    public ApiResponse checkByGoogleEmail(String googleEmail) {
        if (utils.checkEmail(googleEmail)) {
            User user = this.userRepository.findByEmail(googleEmail)
                    .orElseThrow(() -> new ResourceNotFoundException(RestConstants.USER_EMAIL_NOT_FOUND));
            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
            String userToken = jwtUtil.generateToken(userDetails.getUsername());

            return ApiResponse.builder()
                    .status(HttpStatus.OK)
                    .message("AUTHORISED BY GOOGLE EMAIL")
                    .data(userToken)
                    .meta(Map.of("user", this.userMapper.toDto(user)))
                    .build();
        }
        return ApiResponse.builder()
                .status(HttpStatus.BAD_REQUEST)
                .message("EMAIL IS NOT VALID")
                .build();
    }

    @Override
    public ApiResponse updateEmail(String email, String newEmail) {
        User user = this.userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.USER_NOT_FOUND));
        Optional<User> optional = this.userRepository.findByEmail(newEmail);
        if (optional.isPresent()) {
            return ApiResponse.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("EMAIL IS ALREADY EXIST")
                    .build();
        }
        String code = utils.getCode();
        if (utils.sendCodeToMail(newEmail, code)) {
            Optional<Code> codeOptional = this.codeRepository.findByUserId(user.getId());
            if (codeOptional.isPresent()) {
                Code c = codeOptional.get();
                c.setCode(code);
                c.setCreatedAt(LocalDateTime.now());
                this.codeRepository.save(c);
                this.userRepository.save(user);

                return ApiResponse.builder()
                        .status(HttpStatus.OK)
                        .message("ENTER CODE")
                        .build();
            }
        }
        return ApiResponse.builder()
                .status(HttpStatus.BAD_REQUEST)
                .message("SMS YUBORISHDA XATOLIK YUZ BERDI.")
                .build();
    }

    @Override
    public ApiResponse checkUpdatedEmail(String oldEmail, String newEmail, String code) {
        User user = this.userRepository.findByEmail(oldEmail)
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.USER_NOT_FOUND));
        Code co = this.codeRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("KOD TOPILMADI"));
        if (co.getCode().equals(code)) {
            user.setEmail(newEmail);

            this.userRepository.save(user);
            this.codeRepository.save(co);

            return ApiResponse.builder()
                    .status(HttpStatus.OK)
                    .message("EMAIL UPDATED")
                    .build();
        }
        return ApiResponse.builder()
                .status(HttpStatus.BAD_REQUEST)
                .message("CODE IS ERROR")
                .build();
    }

    @Override
    public ApiResponse updatePassword(String email, String newPassword) {
        User user = this.userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.USER_NOT_FOUND));
        String code = utils.getCode();
        if (utils.sendCodeToMail(email, code)) {
            Optional<Code> optional = this.codeRepository.findByUserId(user.getId());
            if (optional.isPresent()) {
                Code c = optional.get();
                c.setCode(code);
                c.setCreatedAt(LocalDateTime.now());
                this.codeRepository.save(c);
                this.userRepository.save(user);
                return ApiResponse.builder()
                        .status(HttpStatus.OK)
                        .message("ENTER CODE")
                        .build();
            }
        }
        return ApiResponse.builder()
                .status(HttpStatus.BAD_REQUEST)
                .message("SOMETHING ERROR TO SEND SMS")
                .build();
    }

    @Override
    public ApiResponse checkUpdatedPassword(String email, String password, String code) {
        User user = this.userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.USER_NOT_FOUND));
        Code co = this.codeRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("CODE IS NOT FOUND"));
        if (co.getCode().equals(code)) {
            user.setPassword(passwordEncoder.encode(password));
            this.userRepository.save(user);

            this.codeRepository.save(co);

            return ApiResponse.builder()
                    .status(HttpStatus.OK)
                    .message("PASSWORD UPDATED SUCCESSFULLY")
                    .build();
        }
        return ApiResponse.builder()
                .status(HttpStatus.BAD_REQUEST)
                .message("CODE IS ERROR")
                .build();
    }

    @Override
    public ApiResponse checkUsername(String username) {
        if (this.utils.existUsername(username)) {
            return ApiResponse.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("USERNAME IS ALREADY EXIST")
                    .build();
        }
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message("SUCCESS")
                .build();
    }

    @Override
    public ApiResponse addRoleToUser(Long userId, Long roleId) {
        User user = this.userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.USER_NOT_FOUND));
        UserRole role = this.roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.ROLE_NOT_FOUND));
        if (user.getRole() != null && !user.getRole().isEmpty()) {
            user.getRole().add(role);
        } else {
            user.setRole(new HashSet<>(Set.of(role)));
        }
        this.userRepository.save(user);
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.ROLE_ADDED_SUCCESSFULLY_TO_USER)
                .build();
    }

    @Override
    public ApiResponse deleteRoleFromUser(Long userId, Long roleId) {
        this.userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.USER_NOT_FOUND));
        this.roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.ROLE_NOT_FOUND));
        this.userRepository.deleteRoleFromUser(userId, roleId);
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message("DELETE ROLE FROM USER")
                .build();
    }

    @Override
    public ApiResponse getUserByToken(String token) {
        String username = jwtUtil.getUsernameFromToken(token);
        User user = this.userRepository.findByPhoneNumberAndDeletedFalse(username)
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.USER_NOT_FOUND));
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .data(this.userMapper.toDto(user))
                .build();
    }

}
