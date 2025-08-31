package org.example.moliyaapp.projection;

import org.example.moliyaapp.enums.Months;

public interface GetEmployeeAmountByMonth {

    Months getMonth();

    Double getTotalAmount();
    Double getFinalAmount();
    Double getUnPaidAmount();
    Double getPaidAmount();
    Double getBonusAmount();


}
