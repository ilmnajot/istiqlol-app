package org.example.moliyaapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.moliyaapp.entity.MonthlyFee;
import org.example.moliyaapp.entity.Transaction;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentDistributionResult {
    private List<MonthlyFee> updatedFees;
    private List<Transaction> newTransactions;
}
