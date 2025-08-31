package org.example.moliyaapp.dto;

import lombok.*;
import org.example.moliyaapp.enums.BonusType;
import org.example.moliyaapp.enums.PaymentType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentBonusDto {
    private Long id;
    private Double amount;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate givenDate;
    private BonusType bonusType;
    private Set<Long> students;
    private Long companyId;

    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean deleted;



    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class createStudentBonus {
        private Double amount;
        private String name;
        private LocalDate startDate;
        private LocalDate endDate;
        private LocalDate givenDate;
        private BonusType bonusType;
        private PaymentType paymentType;
        private Set<Long> students;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateStudentBonus {
        private Double amount;
        private String name;
        private LocalDate startDate;
        private LocalDate endDate;
        private LocalDate givenDate;
        private BonusType bonusType;
        private Set<Long> students;
    }

}