package org.example.moliyaapp.service;

import org.example.moliyaapp.entity.Expenses;

public interface ExpenseGoogleSheet {
    void recordExpense(Expenses expenses);
    void initializeSheet();
    void updateExpense(Expenses expenses);
}
