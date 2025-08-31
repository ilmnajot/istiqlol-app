package org.example.moliyaapp.dto;

import lombok.*;
import org.example.moliyaapp.enums.TransactionType;

@Setter
@Getter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class CompanyDto {

    private TransactionType transactionType;
    private Double amount;
    private Double cashAmount;
    private Double cardAmount;

}
