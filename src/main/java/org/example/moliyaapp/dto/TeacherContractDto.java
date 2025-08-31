package org.example.moliyaapp.dto;

import lombok.*;
import org.example.moliyaapp.enums.SalaryType;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TeacherContractDto {

    private Long id;
    private UserDto userDto;
    private SalaryType salaryType;
    private Integer lessonCountPerMonth;
    private Integer days;
    private Double monthlySalaryOrPerLessonOrPerDay;
    private LocalDate startDate;
    private LocalDate endDate; // null means ongoing
    private Boolean active;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
    private Boolean deleted;

    @Data
    public static class CreateContractDto {
        private Long teacherId;
        private SalaryType salaryType;
        private Double monthlySalaryOrPerLessonOrPerDay;
        private Boolean active;
        private LocalDate startDate;
        private LocalDate endDate; // null means ongoing
    }
}
