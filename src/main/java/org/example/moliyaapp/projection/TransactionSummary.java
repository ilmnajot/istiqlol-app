package org.example.moliyaapp.projection;

import org.example.moliyaapp.enums.TransactionType;

public interface TransactionSummary {
    TransactionType getTransactionType();
    Double getAmount();
    Double getCashAmount();
    Double getCardAmount();
}
