package org.example.moliyaapp.projection;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GetAmountByYearResponse {
    private String year;
    private Double totalAmount;
    private Double paidAmount;
    private Double unPaidAmount;
    private Double cuttingAmount;
}