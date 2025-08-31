package org.example.moliyaapp.projection;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import org.example.moliyaapp.enums.Gender;
import org.example.moliyaapp.enums.Months;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GetAmountByMonthResponse {
    private Months month;
    private Double totalAmount;
    private Double paidAmount;
    private Double unPaidAmount;
    private Double cuttingAmount;


}
