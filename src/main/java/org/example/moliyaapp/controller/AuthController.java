package org.example.moliyaapp.controller;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.example.moliyaapp.dto.*;
import org.example.moliyaapp.service.AuthService;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auths")
public class AuthController {
    private final AuthService authService;

    @Hidden
    @PostMapping("/registerByEmail")
    public HttpEntity<ApiResponse> registerByEmail(@RequestBody RegisterDto dto) {
        ApiResponse apiResponse =  this.authService.registerByEmail(dto);
        return ResponseEntity.status(apiResponse.getStatus()).body(apiResponse);
    }

    @Hidden
    @PostMapping("/verify")
    public ApiResponse verify(@RequestBody VerifyDto dto) {
        return this.authService.verify(dto);
    }

    @Hidden
    @PostMapping("/resendCodeToEmail")
    public ApiResponse resendCodeToEmail(@RequestParam("email") String email) {
        return this.authService.resendCodeToEmail(email);
    }

    @Hidden
    @PostMapping("/checkCodeByEmail")
    public ApiResponse checkCodeByEmail(@RequestParam("email") String email,
                                        @RequestParam("code") String code) {
        return this.authService.checkCodeByEmail(email, code);
    }

//    @PreAuthorize("hasAnyRole('HR','DEVELOPER','ADMIN')")
    @PostMapping("/register-employee")
    public HttpEntity<ApiResponse> registerEmployee(@RequestBody RegisterDto.RegisterEmployee dto) {
        ApiResponse apiResponse = this.authService.registerEmployee(dto);
        return ResponseEntity.status(apiResponse.getStatus()).body(apiResponse);
    }

    @PreAuthorize("hasAnyRole('OWNER','PRINCIPAL','ADMIN', 'RECEPTION','HR','EDUCATIONAL_DEPARTMENT','DEVELOPER','MARKETING')")
    @PutMapping("/change-credentials/{id}")
    public HttpEntity<ApiResponse> changeCredentials(@PathVariable(value = "id") Long id,
                                                     @RequestBody ChangeCredentialDto.CreateCredentialDto dto) {
        ApiResponse apiResponse = this.authService.changeCredentials(dto, id);
        return ResponseEntity.status(apiResponse.getStatus()).body(apiResponse);
    }

//    @PreAuthorize("hasAnyRole('OWNER','PRINCIPAL','ADMIN', 'RECEPTION','HR','EDUCATIONAL_DEPARTMENT','RECEPTION','CASHIER','DEVELOPER')")
    @PostMapping("/login")
    public HttpEntity<ApiResponse> login(@RequestBody LoginDto dto) {
        ApiResponse apiResponse = this.authService.login(dto);
        return ResponseEntity.status(apiResponse.getStatus()).body(apiResponse);
    }

    @Hidden
    @PreAuthorize("hasAnyRole('OWNER','PRINCIPAL','ADMIN', 'RECEPTION','HR','EDUCATIONAL_DEPARTMENT','RECEPTION','CASHIER','DEVELOPER')")
    @PostMapping("refresh")
    public ApiResponse refreshToken(@RequestBody LoginDto.RefreshTokenRequest request) {
        return this.authService.refreshToken(request);
    }

    @Hidden
    @PreAuthorize("hasAnyRole('OWNER','PRINCIPAL','ADMIN', 'RECEPTION','HR','EDUCATIONAL_DEPARTMENT','RECEPTION','CASHIER','DEVELOPER')")
    @PostMapping("/logout")
    public HttpEntity<ApiResponse> logout(@RequestBody LoginDto.LogoutRequest request) {
        ApiResponse apiResponse = this.authService.logout(request);
        return ResponseEntity.status(apiResponse.getStatus()).body(apiResponse);
    }

    ;

    @PreAuthorize("hasAnyRole('OWNER','PRINCIPAL','ADMIN', 'RECEPTION','HR','EDUCATIONAL_DEPARTMENT','RECEPTION','CASHIER','DEVELOPER','MARKETING')")
    @PutMapping("/change-password")
    public HttpEntity<ApiResponse> changePassword(
            @RequestBody ChangeCredentialDto.PasswordDto dto,
            @RequestParam("id") Long id) {
        ApiResponse apiResponse = this.authService.changePassword(dto, id);
        return ResponseEntity.status(apiResponse.getStatus()).body(apiResponse);
    }

    @DeleteMapping("/delete-account/{id}")
    public ApiResponse deleteAccount(@PathVariable Long id) {
        return this.authService.deleteAccount(id);
    }

    @PreAuthorize("hasAnyRole('HR','DEVELOPER','ADMIN','MARKETING')")
    @PutMapping("/update-employee-by-admin")
    public HttpEntity<ApiResponse> updateEmployeeByAdmin(@RequestBody RegisterDto.UpdateEmployee dto) {
        ApiResponse apiResponse = this.authService.updateEmployeeByAdmin(dto);
        return ResponseEntity.status(apiResponse.getStatus()).body(apiResponse);
    }
}
