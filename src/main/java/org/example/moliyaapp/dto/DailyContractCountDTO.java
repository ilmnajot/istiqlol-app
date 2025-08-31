package org.example.moliyaapp.dto;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class DailyContractCountDTO {
    private int day;
    private long count;


    // Getters and setters (or use Lombok @Data)
}
