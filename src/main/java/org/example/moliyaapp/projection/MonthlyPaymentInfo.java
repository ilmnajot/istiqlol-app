package org.example.moliyaapp.projection;

import org.example.moliyaapp.enums.Months;

public interface MonthlyPaymentInfo {

    Months getMonths();
    String getTariffName();
    Double getTotalFee();
    Double getAmountPaid();
    Double getRemainingBalance();

}
