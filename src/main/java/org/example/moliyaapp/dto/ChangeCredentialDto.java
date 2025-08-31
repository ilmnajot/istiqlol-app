package org.example.moliyaapp.dto;

import lombok.*;

import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChangeCredentialDto {

    private Long id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private Long updatedBy;
    private Long createdBy;
    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;
    private Boolean deleted;

    @Data
    public static class CreateCredentialDto {
        private String fullName;
        private String email;
        private String phoneNumber;
    }

    @Data
    public static class LoginDto {
        private String phone;
        private String password;
    }

    @Data
    public static class PasswordDto {
        private String oldPassword;
        private String newPassword;
    }
}
