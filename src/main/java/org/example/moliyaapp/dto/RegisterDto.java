package org.example.moliyaapp.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.moliyaapp.enums.Role;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterDto {
    private String email;
    private String password;
    private String fullName;
    private String contractNumber;
    private String role;

    @Data
    public static class RegisterEmployee {

        private String fullName;
        private String email; //optional
        private String phone;
        private String password;
        private String contractNumber;
        private Role roleName;
    }
        @Data
        public static class UpdateEmployee {

            private Long employeeId;
            private String fullName;
            private String email; //optional
            private String phone;
            private Role roleName;
            private String contractNumber;

        }
}
