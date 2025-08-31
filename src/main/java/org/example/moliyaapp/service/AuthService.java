package org.example.moliyaapp.service;

import org.example.moliyaapp.dto.*;
import org.springframework.stereotype.Component;

@Component
public interface AuthService {

//    ApiResponse loginByEmail(LoginDto dto);

    ApiResponse registerByEmail(RegisterDto dto);

    ApiResponse verify(VerifyDto dto);

    ApiResponse resendCodeToEmail(String email);

    ApiResponse checkCodeByEmail(String email, String code);

    ApiResponse registerEmployee(RegisterDto.RegisterEmployee dto);

    ApiResponse changeCredentials(ChangeCredentialDto.CreateCredentialDto dto, Long id);

    ApiResponse login(LoginDto dto);

    ApiResponse deleteAccount(Long id);

    ApiResponse changePassword(ChangeCredentialDto.PasswordDto dto, Long id);

    ApiResponse updateEmployeeByAdmin(RegisterDto.UpdateEmployee dto);

    ApiResponse refreshToken(LoginDto.RefreshTokenRequest request);

    ApiResponse logout(LoginDto.LogoutRequest request);
}
