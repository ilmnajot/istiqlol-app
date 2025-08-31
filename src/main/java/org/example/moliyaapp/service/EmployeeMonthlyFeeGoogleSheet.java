package org.example.moliyaapp.service;

import org.example.moliyaapp.entity.MonthlyFee;

public interface EmployeeMonthlyFeeGoogleSheet {

    void recordEmployeeMonthlyFee(MonthlyFee monthlyFee);
    void initializeSheet();
    void updateEmployeeMonthlyFee(MonthlyFee monthlyFee);
}
