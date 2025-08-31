package org.example.moliyaapp.projection;

import lombok.*;
import org.example.moliyaapp.enums.Gender;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@Builder
public class StudentsByTariff {
    private String tariffName;
    private Double amount;
    private int count;
}
