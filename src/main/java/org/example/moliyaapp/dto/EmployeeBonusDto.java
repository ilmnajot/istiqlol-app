package org.example.moliyaapp.dto;

import lombok.*;
import org.example.moliyaapp.enums.BonusType;
import org.example.moliyaapp.enums.PaymentType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EmployeeBonusDto {

    private Long id;
    private Double amount;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate givenDate;
    private BonusType bonusType;
    private Set<Long> employees;
    private Long companyId;

    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean deleted;

    @Setter
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class createEmployeeBonus {
        private Double amount;
        private String name;
        private LocalDate startDate;
        private LocalDate endDate;
        private LocalDate givenDate;
        private LocalDateTime createdAt;
        private BonusType bonusType;
        private Set<Long> employees;
        private PaymentType paymentType;
        private Long companyId;
    }
    @Setter
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class UpdateEmployeeBonus {
        private Double amount;
        private String name;
        private LocalDate startDate;
        private LocalDate endDate;
        private LocalDate givenDate;
        private LocalDateTime updatedAt;
        private BonusType bonusType;
        private Set<Long> employees;
        private PaymentType paymentType;
        private Long companyId;


    }

}
