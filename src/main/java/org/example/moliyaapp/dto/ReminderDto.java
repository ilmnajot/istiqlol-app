package org.example.moliyaapp.dto;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import org.example.moliyaapp.entity.StudentContract;
import org.example.moliyaapp.enums.Gender;
import org.example.moliyaapp.enums.Grade;
import org.example.moliyaapp.enums.Months;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.*;
import jakarta.persistence.Entity;
import org.example.moliyaapp.enums.StudentGrade;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReminderDto {

    private Long id;
    private Long studentContractId;
    private Months month;
    private Boolean isReminded;
    private LocalDateTime estimatedTime;
    private String comment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
    private Boolean deleted;

    @Data
    public static class ReminderCreateAndUpdateDto {
        private Months month;
        private Boolean isReminded;
        private LocalDateTime estimatedTime;
        private String comment;
    }

    @Data
    @Builder
    public static class ReminderFilterResponse {

        private Long id;
        private Long studentContractId;
        private String studentName;
        private Gender gender;
        private StudentGrade grade;
        private Boolean status;

        private Grade stGrade;
        private String academicYear;
        private Months month;
        private Boolean isReminded;
        private LocalDateTime estimatedTime;
        private String comment;
        private String phone1;
        private String phone2;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private Long createdBy;
        private Long updatedBy;
        private Boolean deleted;


    }
}
