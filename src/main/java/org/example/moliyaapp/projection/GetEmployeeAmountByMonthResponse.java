package org.example.moliyaapp.projection;

import lombok.*;
import org.example.moliyaapp.enums.Months;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@Builder
public class GetEmployeeAmountByMonthResponse {
    private Months month;
    private Double totalAmount;
    private Double finalAmount;
    private Double paidAmount;
    private Double unPaidAmount;
//    private Double cuttingAmount;
    private Double bonusAmount;


}
