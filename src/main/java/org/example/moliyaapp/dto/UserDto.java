package org.example.moliyaapp.dto;

import lombok.*;
import org.example.moliyaapp.enums.SalaryType;
import org.example.moliyaapp.enums.UserStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String email;
    private String phoneNumber;
    private String password;
    private String fullName;
    private String contractNumber; //

    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean deleted;


    private UserStatus status;

    private List<String> roles = new ArrayList<>();

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateUser {
        private String fullName;
        private String phoneNumber;
        private String contractNo;
        private String roleName;


    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateStudent {
        private String phoneNumber;
        private String fullName;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateEmployee {
        private String fullName;
        private String email;
        private String phoneNumber;
        private String contractNo;
        private Set<String> roleNames;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmployeeResponse {
        private Long id;
        private String fullName;
        private String email;
        private String phoneNumber;
        private String contractNo;
        private Set<String> roleNames;
        private UserStatus userStatus;
        private List<TeacherTableDto> teacherTableDtoList;
        private TeacherContractDto teacherContractDto;

        private Long createdBy;
        private Long updatedBy;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private boolean deleted;


    }
}
