package org.example.moliyaapp.projection;

import org.example.moliyaapp.dto.StudentTariffDto;
import org.example.moliyaapp.enums.Months;
import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@Builder
public class MonthlyPaymentInfoDto {

    private Months month;
    private String tariffName;
    private Double totalAmount;
    private Double paidAmount;
    private Double leftAmount;

    public MonthlyPaymentInfoDto(
            Months month,
            String tariffName,
            Double totalAmount,
            Double paidAmount,
            Double leftAmount) {
        this.month = month;
        this.tariffName = tariffName;
        this.totalAmount = totalAmount;
        this.paidAmount = paidAmount;
        this.leftAmount = leftAmount;
    }



}
