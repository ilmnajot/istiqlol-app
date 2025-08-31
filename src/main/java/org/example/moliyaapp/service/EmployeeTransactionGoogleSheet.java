package org.example.moliyaapp.service;

import org.example.moliyaapp.entity.Transaction;

public interface EmployeeTransactionGoogleSheet {
    void recordEmployeeTransactions(Transaction transaction);
    void initializeSheet();
    void updateEmployeeTransaction(Transaction transaction);
}

