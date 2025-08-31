package org.example.moliyaapp.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.moliyaapp.dto.*;
import org.example.moliyaapp.entity.Code;
import org.example.moliyaapp.entity.User;
import org.example.moliyaapp.entity.UserRole;
import org.example.moliyaapp.enums.Role;
import org.example.moliyaapp.enums.UserStatus;
import org.example.moliyaapp.exception.ResourceNotFoundException;
import org.example.moliyaapp.mapper.CredentialMapper;
import org.example.moliyaapp.mapper.UserMapper;
import org.example.moliyaapp.repository.CodeRepository;
import org.example.moliyaapp.repository.RoleRepository;
import org.example.moliyaapp.repository.UserRepository;
import org.example.moliyaapp.service.AuthService;
import org.example.moliyaapp.service.EmployeeGoogleSheet;
import org.example.moliyaapp.utils.JwtUtil;
import org.example.moliyaapp.utils.RestConstants;
import org.example.moliyaapp.utils.Utils;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final CodeRepository codeRepository;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final Utils utils;
    private final CredentialMapper credentialMapper;
    private final EmployeeGoogleSheet employeeGoogleSheet;
//    private final RedisTemplate<String, String> redisTemplate;


//    public void saveRefreshToken(String phone, String refreshToken) {
//        redisTemplate.opsForValue().set(
//                "refresh_token:" + phone,
//                refreshToken,
//                this.jwtUtil.getRefreshTokenExpirationMs(),
//                TimeUnit.MILLISECONDS
//        );
//    }
//
//    public String getRefreshToken(String phone) {
//        return redisTemplate.opsForValue().get("refresh_token:" + phone);
//    }
//
//    public void deleteRefreshToken(String phone) {
//        redisTemplate.delete("refresh_token:" + phone);
//    }
//
//    public void addToBlackList(String token, long expirationMs) {
//        redisTemplate.opsForValue().set(
//                "blacklist:" + token,
//                "true",
//                expirationMs,
//                TimeUnit.MILLISECONDS
//        );
//    }
//
//    public boolean isTokenBlacklisted(String token) {
//        return redisTemplate.hasKey("blacklist:" + token).equals(Boolean.TRUE);
//    }

    @Transactional
    @Override
    public ApiResponse registerByEmail(RegisterDto dto) {
        User user = this.userRepository.findByEmail(dto.getEmail()).orElse(null);
        if (user != null) {
            return ApiResponse.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message(RestConstants.EMAIL_ALREADY_EXISTS)
                    .build();
        }
        if (!utils.checkEmail(dto.getEmail())) {
            return ApiResponse.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("NOTO'G'RI EMAIL")
                    .build();
        }
        String code = utils.getCode();
        if (utils.sendCodeToMail(dto.getEmail(), code)) {
            try {
                UserRole role = this.roleRepository.findByNameAndDeletedFalse(dto.getRole()).orElse(null);

                User entity = this.userMapper.toEntityByEmail(dto);
                entity.setCreatedAt(LocalDateTime.now());
                entity.setStatus(UserStatus.PENDING);
                if (role != null) {
                    if (entity.getRole() != null && !entity.getRole().isEmpty()) {
                        entity.getRole().add(role);
                    } else {
                        Set<UserRole> set = new HashSet<>();
                        set.add(role);
                        entity.setRole(set);
                    }
                }
                User save = this.userRepository.save(entity);
//                this.deviceService.addTrustedDevice(save.getId(), dto.getDeviceId());

                Code c = Code.builder()
                        .user(save)
                        .code(code)
                        .build();
                this.codeRepository.save(c);

                return ApiResponse.builder()
                        .status(HttpStatus.OK)
                        .message("KOD MUVAFFAQIYATLI YUBORILDI")
                        .meta(Map.of("userId", save.getId()))
                        .build();

            } catch (Exception e) {
                return ApiResponse.builder()
                        .status(HttpStatus.BAD_REQUEST)
                        .message(e.getMessage())
                        .build();
            }
        }
        return ApiResponse.builder()
                .status(HttpStatus.BAD_REQUEST)
                .message("SMS YUBORISHDA XATOLIK YUZ BERDI.")
                .build();
    }

    @Transactional
    @Override
    public ApiResponse verify(VerifyDto dto) {
        User user = this.userRepository.findById(dto.getId())
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.USER_NOT_FOUND));
        Map<String, Object> map = new HashMap<>();
        Code code = this.codeRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("KOD TOPILMADI"));
        if (code.getCode().equals(dto.getCode())) {
            this.codeRepository.save(code);

            user.setPassword(passwordEncoder.encode(dto.getPassword()));
            user.setStatus(UserStatus.ACTIVE);
            if (dto.getEmail() != null && !dto.getEmail().trim().isEmpty()) {
                user.setEmail(dto.getEmail());
            }
            User saveUser = this.userRepository.save(user);


            UserDetails userDetails = userDetailsService.loadUserByUsername(saveUser.getUsername());
            String generateToken = jwtUtil.generateToken(userDetails.getUsername());

            map.put("token", generateToken);
            map.put("expiredDate", jwtUtil.getAccessTokenExpiredDate(generateToken));
            map.put("userId", saveUser.getId());
            map.put("user", this.userMapper.toDto(saveUser));
            return ApiResponse.builder()
                    .status(HttpStatus.OK)
                    .message("Muvaffaqiyatli tasdiqlandi.")
                    .data(map)
                    .build();

        }
        return ApiResponse.builder()
                .status(HttpStatus.BAD_REQUEST)
                .message("CODE IS ERROR")
                .build();
    }

//    @Override
//    public ApiResponse loginByEmail(LoginDto dto) {
//        User user = this.userRepository.findByEmail(dto.getEmail())
//                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.USER_NOT_FOUND));
//
//        if (!utils.checkEmail(dto.getEmail())) {
//            return ApiResponse.builder()
//                    .status(HttpStatus.BAD_REQUEST)
//                    .message("INVALID EMAIL")
//                    .build();
//        }
//        try {
//            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getPassword());
//            authenticationManager.authenticate(
//                    authentication
//            );
//        } catch (Exception e) {
//            return ApiResponse.builder()
//                    .status(HttpStatus.BAD_REQUEST)
//                    .message("NOTO'G'RI PAROL")
//                    .build();
//        }
//        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
//        String token = jwtUtil.generateToken(userDetails.getUsername());
//
//        Device trustedDevice = this.deviceRepository.findByUserIdAndDeviceName(user.getId(), dto.getDeviceId())
//                .orElse(null);
//
//        String code = utils.getCode();
//        if (trustedDevice == null) {
//            if (utils.sendCodeToMail(dto.getEmail(), code)) {
//                User save = this.userRepository.save(user);
//                this.deviceService.addTrustedDevice(user.getId(), dto.getDeviceId());
//
//                Code co = this.codeRepository.findByUserId(save.getId()).orElse(null);
//                if (co != null) {
//                    co.setCode(code);
//                    this.codeRepository.save(co);
//                }
//
//                return ApiResponse.builder()
//                        .code(0)
//                        .status(HttpStatus.OK)
//                        .message(token)
//                        .data(save.getId())
//                        .meta(Map.of("user", this.userMapper.toDto(save),
//                                "expiredAt", jwtUtil.getAccessTokenExpiredDate(token)))
//                        .build();
//            }
//        }
//        return ApiResponse.builder()
//                .code(1)
//                .status(HttpStatus.OK)
//                .message(token)
//                .data(user.getId())
//                .meta(Map.of("user", this.userMapper.toDto(user),
//                        "expiredAt", jwtUtil.getAccessTokenExpiredDate(token)))
//                .build();
//    }

    @Override
    public ApiResponse resendCodeToEmail(String email) {
        User user = this.userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.USER_NOT_FOUND));
        String code = utils.getCode();
        if (utils.sendCodeToMail(email, code)) {
            Code c = this.codeRepository.findByUserId(user.getId()).orElse(null);
            if (c != null) {
                c.setCode(code);
                c.setCreatedAt(LocalDateTime.now());
                this.codeRepository.save(c);
            }
            return ApiResponse.builder()
                    .status(HttpStatus.OK)
                    .message("KODE MUVAFFAQIYATLI YUBORILDI.")
                    .build();
        }
        return ApiResponse.builder()
                .status(HttpStatus.BAD_REQUEST)
                .message("KOD YUBORISHDA XATOLIK YUZ BERDI.")
                .build();
    }

    @Override
    public ApiResponse checkCodeByEmail(String email, String code) {
        String token = "";
        if (this.utils.checkCode(email, code)) {
            User user = this.userRepository.findByEmail(email).orElse(null);
            if (user != null) {
                token = jwtUtil.generateToken(user.getUsername());
                Code c = this.codeRepository.findByUserId(user.getId()).orElse(null);
                if (c != null) {
//                    c.setApprovedAt(LocalDateTime.now());
                    this.codeRepository.save(c);
                }
            }
            return ApiResponse.builder()
                    .status(HttpStatus.OK)
                    .message("SUCCESS")
                    .data(this.userMapper.toDto(user))
                    .meta(Map.of("Token", token))
                    .build();
        }
        return ApiResponse.builder()
                .status(HttpStatus.BAD_REQUEST)
                .message("CODE IS ERROR")
                .build();
    }

    @Override
    public ApiResponse login(LoginDto dto) {
        User user = this.userRepository.findByPhoneNumberAndDeletedFalse(dto.getPhone())
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.USER_NOT_FOUND));
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getPhoneNumber());
        try {
            this.authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    dto.getPhone(),
                    dto.getPassword()
            ));
        } catch (Exception e) {
            return ApiResponse.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .data(e.getMessage())
                    .build();
        }
        String token = this.jwtUtil.generateAccessToken(userDetails.getUsername());
//        String refreshToken = this.jwtUtil.generateRefreshToken(userDetails.getUsername());
//        this.saveRefreshToken(dto.getPhone(), refreshToken);

        return ApiResponse
                .builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESS)
                .data(this.userMapper.toDto(user))
                .meta(Map.of(
                        "Token", token,
                        "expiredAt", this.jwtUtil.getAccessTokenExpiredDate(token)
//                        "token", token,
//                        "refreshToken", refreshToken,
//                        "accessTokenExpire", this.jwtUtil.getRefreshTokenExpiredDate(token),
//                        "refreshTokenExpire", this.jwtUtil.getAccessTokenExpiredDate(accessToken)
                ))
                .build();
    }

    @Override
    public ApiResponse registerEmployee(RegisterDto.RegisterEmployee dto) {
        log.info("ROLLAR {}", SecurityContextHolder.getContext().getAuthentication().getAuthorities().toString());
        Optional<User> userOptional = this.userRepository.findByPhoneNumberAndDeletedFalse(dto.getPhone());
        if (userOptional.isPresent()) {
            return ApiResponse.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("FOYDALANUVCHI ALLAQACHON RO'YXATDAN O'TKAZILGAN!")
                    .build();
        }

        UserRole userRole = this.roleRepository.findByNameAndDeletedFalse(dto.getRoleName().name())
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.ROLE_NOT_FOUND));

        Role requestedRole = Role.valueOf(userRole.getName());

        User user = this.userMapper.toEntity(dto);
        if (user.getRole() != null && !user.getRole().isEmpty()) {
            user.getRole().add(userRole);
        } else {
            Set<UserRole> set = new HashSet<>();
            set.add(userRole);
            user.setRole(set);
        }

        user.setStatus(UserStatus.ACTIVE);

        if (requestedRole.equals(Role.EMPLOYEE) || requestedRole.equals(Role.TEACHER)) {
            user.setPassword(null);
        }
        user.setPassword(this.passwordEncoder.encode(dto.getPassword()));
        User saved = this.userRepository.save(user);

        try {
            employeeGoogleSheet.initializeSheet();
            this.employeeGoogleSheet.recordEmployee(saved);
        } catch (RuntimeException e) {
                log.error(e.getMessage());
        }
        UserDto userDto = this.userMapper.toDto(saved);

        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESSFULLY_REGISTERED)
                .data(userDto)
                .build();
    }

    @Transactional
    @Override
    public ApiResponse changeCredentials(ChangeCredentialDto.CreateCredentialDto dto, Long id) {
        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
        User existingUser = this.userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.USER_NOT_FOUND));

        if (!existingUser.getUsername().equals(currentUser)) {
            return ApiResponse.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message(RestConstants.FAILED_TO_UPDATE)
                    .build();
        }
        credentialMapper.toUpdate(dto, existingUser);

        System.out.println("Before Save: " + existingUser);
        User savedUser = this.userRepository.save(existingUser);
        try {
        employeeGoogleSheet.initializeSheet();
        employeeGoogleSheet.updateEmployee(savedUser);
        } catch (RuntimeException e) {
            log.error(e.getMessage());
        }
        ChangeCredentialDto credentialDto = this.credentialMapper.toDto(savedUser);
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.USER_UPDATED)
                .data(credentialDto)
                .build();
    }


    @Override
    public ApiResponse deleteAccount(Long id) {
        User user = this.userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.USER_NOT_FOUND));
        user.setDeleted(true);

        Set<UserRole> roles = user.getRole();
        for (UserRole role : roles) {
            this.userRepository.deleteRoleFromUser(user.getId(), role.getId());
        }
        user.setStatus(UserStatus.PENDING);
        this.userRepository.save(user);
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.USER_DELETED)
                .build();
    }

    @Override
    public ApiResponse changePassword(ChangeCredentialDto.PasswordDto dto, Long id) {
        User targetUser = this.userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.USER_NOT_FOUND));
        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = this.userRepository.findByPhoneNumberAndDeletedFalse(currentUser)
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.USER_NOT_FOUND));
        if (!targetUser.getId().equals(user.getId())) {
            return ApiResponse.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("SIZ FAQAT O'Z PAROLINGIZNI O'ZGARTIRA OLASIZ!")
                    .build();
        }

        if (dto.getNewPassword() == null || dto.getNewPassword().trim().isEmpty()) {
            return ApiResponse.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("YANGI PAROL KIRITILMAGAN")
                    .build();
        }

        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            return ApiResponse.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("ESKI PAROL XATO!")
                    .build();
        }
        if (passwordEncoder.matches(dto.getNewPassword(), user.getPassword())) {
            return ApiResponse.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("ESKI PAROL BILAN YANGI PAROL BIR XIL BO'LMASLIGI KERAK.")
                    .build();
        }
        if (dto.getNewPassword() == null || dto.getNewPassword().trim().isEmpty()) {
            return ApiResponse.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("YANGI PAROL JOYI BO'SH BO'LMASLIGI KERAK")
                    .build();
        }

        if (dto.getNewPassword().length() < 6) {
            return ApiResponse.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("YANGI PAROL UZUNLIGI KAMIDA 6 TA BO'LISHI KERAK")
                    .build();
        }

        user.setPassword(this.passwordEncoder.encode(dto.getNewPassword()));
        this.userRepository.save(user);
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.PASSWORD_SUCCESSFULLY_UPDATED)
                .build();
    }

    @Transactional
    @Override
    public ApiResponse updateEmployeeByAdmin(RegisterDto.UpdateEmployee dto) {

        User user = this.userRepository.findByIdAndDeletedFalse(dto.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.USER_NOT_FOUND));

        UserRole userRole = this.roleRepository.findByNameAndDeletedFalse(dto.getRoleName().name())
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.ROLE_NOT_FOUND));

        Optional<UserRole> optionalUserRole = this.roleRepository.findByNameAndDeletedFalse(userRole.getName());

        Role requestedRole = Role.valueOf(optionalUserRole.get().getName());
        if (requestedRole != Role.EMPLOYEE && requestedRole != Role.TEACHER) {
            boolean roleTaken = userRepository.existsByNameAndDeletedFalse(requestedRole.name(), user.getId());
            if (roleTaken) {
                return ApiResponse.builder()
                        .status(HttpStatus.BAD_REQUEST)
                        .message("BU ROLE ALLAQACHON BOSHQA FOYDALANUVCHIGA BERILGAN!")
                        .build();
            }
        }

        if (dto.getFullName() != null && !dto.getFullName().trim().isEmpty()) {
            user.setFullName(dto.getFullName());
        }
        if (dto.getEmail() != null && !dto.getEmail().trim().isEmpty()) {
            user.setEmail(dto.getEmail());
        }
        if (dto.getPhone() != null && !dto.getPhone().trim().isEmpty()) {
            user.setPhoneNumber(dto.getPhone());
        }
        if (dto.getRoleName() != null && !dto.getRoleName().name().trim().isEmpty()) {
            user.getRole().clear();
            user.getRole().add(userRole);
        } else {
            user.setRole(new HashSet<>(Set.of(userRole)));
        }
        if (dto.getContractNumber() != null && !dto.getContractNumber().trim().isEmpty()) {
            user.setContractNumber(dto.getContractNumber());
        }
        User saved = this.userRepository.save(user);
        try {
            this.employeeGoogleSheet.initializeSheet();
            this.employeeGoogleSheet.updateEmployee(saved);
        }catch (Exception e){
            log.error(e.getMessage());
        }
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESSFULLY_UPDATED)
                .data(saved)
                .build();
    }

    @Override
    public ApiResponse refreshToken(LoginDto.RefreshTokenRequest request) {
        String oldRefreshToken = request.getRefreshToken();
        String phone = this.jwtUtil.extractUsername(oldRefreshToken);
//        String storedRefreshToken = this.getRefreshToken(phone);
//        if (storedRefreshToken == null || !storedRefreshToken.equals(oldRefreshToken)) {
//            return ApiResponse.builder()
//                    .status(HttpStatus.UNAUTHORIZED)
//                    .message("Refresh token not found or does not match")
//                    .build();
//        }
        UserDetails userDetails = this.userDetailsService.loadUserByUsername(phone);

        String newRefreshToken = jwtUtil.generateRefreshToken(userDetails.getUsername());
        String newAccessToken = jwtUtil.generateAccessToken(userDetails.getUsername());


//        this.saveRefreshToken(phone, newRefreshToken);
        return ApiResponse.builder()
                .status(HttpStatus.CREATED)
                .message(RestConstants.SUCCESS)
                .data(Map.of(
                        "refreshToken", newRefreshToken,
                        "accessToken", newAccessToken,
                        "refreshTokenExpire", this.jwtUtil.getRefreshTokenExpiredDate(newRefreshToken),
                        "accessTokenExpire", this.jwtUtil.getAccessTokenExpiredDate(newAccessToken))
                )
                .build();
    }

    @Override
    public ApiResponse logout(LoginDto.LogoutRequest request) {
        try {
            // 1. Foydalanuvchi mavjudligini tekshirish
            if (!userRepository.existsByPhoneNumberAndDeletedFalse(request.getPhone())) {
                throw new ResourceNotFoundException("User not found");
            }

//            // 2. Access tokenni validatsiya qilish
//            if (jwtUtil.validateToken(request.getAccessToken(), request.getPhone())) {
//                // 3. Access tokenni blacklistga qo'shish
//                this.addToBlackList(
//                        request.getAccessToken(),
//                        jwtUtil.getAccessTokenExpirationMs()
//                );
//            }

            // 4. Refresh tokenni o'chirish
//            this.deleteRefreshToken(request.getPhone());

            return ApiResponse.builder()
                    .status(HttpStatus.OK)
                    .message("Successfully logged out")
                    .build();

        } catch (ResourceNotFoundException e) {
            return ApiResponse.builder()
                    .status(HttpStatus.NOT_FOUND)
                    .message(e.getMessage())
                    .build();
        } catch (Exception e) {
            return ApiResponse.builder()
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .message("Logout failed: " + e.getMessage())
                    .build();
        }
    }


    public User currentUser() {
        String name = SecurityContextHolder.getContext().getAuthentication().getName();
        return this.userRepository.findByPhoneNumberAndDeletedFalse(name)
                .orElseThrow(() -> new ResourceNotFoundException("FOYDALANUVCHI TOPILMADI"));
    }
}
