package org.example.moliyaapp.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginDto {
    private String phone;
    private String password;


    @Data
    public static class AuthResponse{
        private String accessToken;
        private String refreshToken;
    }
    @Data
    public static class RefreshTokenRequest{
        private String refreshToken;
    }
    @Data
    public static class LogoutRequest{
        private String phone;
        private String accessToken;
    }
}
