package org.example.moliyaapp.dto;

import lombok.*;
import org.example.moliyaapp.enums.Months;
import org.example.moliyaapp.enums.PaymentStatus;
import org.example.moliyaapp.enums.PaymentType;
import org.example.moliyaapp.enums.StudentGrade;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class MonthlyFeeDto {

    private Long id;
    private Months months;
    private Double totalFee;  // Total required amount (e.g., 300 USD per month)
    private Double amountPaid;  // Total amount student has paid so far
    private Double remainingBalance;  // Amount left to pay (totalFee - amountPaid)
    private Double bonus; //premiya
    private Double penalty; //shtraf
    private Double discount; //
    private PaymentStatus paymentStatus;  // FULLY_PAID, PARTIALLY_PAID, UNPAID
    private Long employeeId;
    private String employeeName; //new
    private Long studentContractId;
    private String studentName;//new
    private String description;
    private String categoryName;
//    private PaymentType paymentType;

    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean deleted;


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateMonthlyFee {
        private Long studentContractId;
        private Months months;
        private Double totalFee;
        private Double amountPaid;
        private Double bonus;
        private Double penalty;
        private PaymentType paymentType;
        private Long employeeId;
        private String description;
        private StudentGrade grade;
        private String categoryName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateMonthlyFee {

        private Months months;
        private Double amountPaid;
        private Long studentContractId;
        private Long employeeId;
        private String description;
        private PaymentType paymentType;
    }


}
