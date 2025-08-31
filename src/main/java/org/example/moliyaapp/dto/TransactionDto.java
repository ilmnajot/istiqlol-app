package org.example.moliyaapp.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.moliyaapp.enums.Months;
import org.example.moliyaapp.enums.PaymentType;
import org.example.moliyaapp.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionDto {
    private Long id;
    private TransactionType transactionType;
    private PaymentType paymentType;
    private Double amount;
    private String description;

    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean deleted;


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TransactionResponseWithCuttingAmountDto {
        private List<TransactionDto> transactions;
        private List<StudentCutAmountDto> cuttingAmounts;
    }

    @Data
    public static class ToFilterDto {
        private Long id;
        private Long studentContractId;
        private TransactionType transactionType;
        private PaymentType paymentType;
        private Double amount;
        private String description;
        private String studentFullName;
        private String employeeFullName;
        private Months month;
        private String amountType; // NEW: To show if it's MAIN or BONUS
        private String academicYear;
        private String uniqueId;

        private Long createdBy;
        private Long updatedBy;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private Boolean deleted;

    }

    @Data
    public static class TransactionOverview {
        private PeriodBreakdown income;
        private PeriodBreakdown outcome;
        private Integer year;
    }

    @Builder
    @Data
    public static class PeriodBreakdown {
        private BigDecimal daily;
        private BigDecimal weekly;
        private BigDecimal monthly;
        private BigDecimal yearly;
    }
    @Data
    @Builder
    public static class HourlyAmountData{
        private Integer hour;
        private String hourLabel;
        private BigDecimal amount;
        private Integer transactionCount;
    }

    @Builder
    @Data
    public static class ChartDataPoint {

        private Integer id;
        private String label;
        private BigDecimal amount;
    }
}
