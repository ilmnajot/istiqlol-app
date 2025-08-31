package org.example.moliyaapp.service;

import org.example.moliyaapp.entity.Transaction;

public interface StudentTransactionGoogleSheet {
    void recordStudentTransactions(Transaction transaction);
    void initializeSheet();
    void updateStudentTransaction(Transaction transaction);
}
