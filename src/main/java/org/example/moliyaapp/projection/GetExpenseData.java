package org.example.moliyaapp.projection;

import org.example.moliyaapp.enums.PaymentType;

public interface GetExpenseData {

    String getCategoryName();
    Double getAmount();
    PaymentType getPaymentType();

}
