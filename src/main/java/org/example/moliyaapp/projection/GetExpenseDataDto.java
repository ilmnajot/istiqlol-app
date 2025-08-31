package org.example.moliyaapp.projection;

import lombok.*;
import org.example.moliyaapp.enums.PaymentType;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GetExpenseDataDto {

    private String categoryName;
    private Double amount;
    private PaymentType paymentType;

}
