package org.example.moliyaapp.projection;

import org.example.moliyaapp.enums.Months;

public interface GetAmountByMonth {

    String getMonth();
//    Months getMonth();

    Double getTotalAmount();
    Double getUnPaidAmount();

    Double getPaidAmount();
    Double getCuttingAmount();


}
