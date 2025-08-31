package org.example.moliyaapp.dto;

import lombok.*;
import org.example.moliyaapp.entity.UserRole;
import org.example.moliyaapp.enums.Months;
import org.example.moliyaapp.enums.PaymentType;
import org.example.moliyaapp.enums.Role;
import org.example.moliyaapp.enums.SalaryType;

import java.util.Set;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TableDto {

    private Long employeeId;
    private String fullName;
    private Set<UserRole> role;
    private Months months;
    private Double monthlySalary;
    private int workDays;
    private int extraDaysOrLessons;
    private int missedDaysOrLessons;

    private int workedDays;
    private Double calculatedSalary;
    private Double finalSalary;
    private PaymentType paymentType;
    private Double cardAmount;
    private Double cashAmount;
    private Double leftAmount;
    private String comment;
    private SalaryType salaryType;
    private CategoryDto categoryDto;

    public TableDto(Long employeeId,
                    String fullName,
                    Months months,
                    Double monthlySalary,
                    int workDays,
                    int extraDaysOrLessons,
                    int missedDaysOrLessons) {

        this.employeeId = employeeId;
        this.fullName = fullName;
        this.months = months;
        this.monthlySalary = monthlySalary;
        this.workDays = workDays;
        this.extraDaysOrLessons = extraDaysOrLessons;
        this.missedDaysOrLessons = missedDaysOrLessons;
    }
}
