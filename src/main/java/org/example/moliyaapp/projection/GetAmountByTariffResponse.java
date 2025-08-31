package org.example.moliyaapp.projection;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import org.example.moliyaapp.enums.Months;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GetAmountByTariffResponse {
    private String quarter;
    private Months month;
    private Double totalAmount;
    private Double paidAmount;
    private Double unPaidAmount;
    private Double cuttingAmount;


    public GetAmountByTariffResponse(String quarter, double total, double paid, double unPaid, double cut) {
            this.quarter =quarter;
            this.totalAmount = total;
            this.paidAmount = paid;
            this.unPaidAmount = unPaid;
            this.cuttingAmount = cut;
    }
}
