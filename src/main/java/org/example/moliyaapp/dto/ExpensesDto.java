package org.example.moliyaapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.moliyaapp.enums.PaymentType;
import org.example.moliyaapp.enums.TransactionType;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpensesDto {

    private Long id;

    private String name;
    private Double amount;
    private String receiver;
    private String spender;
    private String description;
    private PaymentType paymentType;
    private TransactionType transactionType;
    private Long companyId;
    private String categoryName;
    private List<String> imageUrls;

    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean deleted;


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateExpense {

        private String name;
        private Double amount;
        private String receiver;
        private String spender;
        private PaymentType paymentType;
        private TransactionType transactionType;
        private String description;
        private String categoryName;
        private LocalDateTime time;
        private List<String> imageUrls;

    }

    @Data
    public static class UpdateExpense {
        private String name;
        private Double amount;
        private String receiver;
        private String spender;
        private String description;
        private PaymentType paymentType;
        private TransactionType transactionType;
        private String categoryName;
    }

}
