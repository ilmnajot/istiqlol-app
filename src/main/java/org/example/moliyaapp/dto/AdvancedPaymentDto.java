package org.example.moliyaapp.dto;

import lombok.*;
import jakarta.persistence.Entity;
import org.example.moliyaapp.enums.Months;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AdvancedPaymentDto {
    private Double amount;
    private Months month;
    private String comment;
    private Boolean isAdvanced = Boolean.FALSE;
}
