package org.example.moliyaapp.service;

import org.example.moliyaapp.entity.MonthlyFee;

public interface StudentMonthlyFeeGoogleSheet {

    void recordMonthlyFee(MonthlyFee monthlyFee);
    void initializeSheet();
    void updateMonthlyFee(MonthlyFee monthlyFee);
}
