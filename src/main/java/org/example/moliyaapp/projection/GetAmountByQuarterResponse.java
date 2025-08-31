package org.example.moliyaapp.projection;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GetAmountByQuarterResponse {
    private String quarter;
    private Double totalAmount;
    private Double paidAmount;
    private Double unPaidAmount;
    private Double cuttingAmount;
}