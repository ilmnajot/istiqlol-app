package org.example.moliyaapp.dto;

import lombok.*;


@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MonthlyStatisticsDto {

    private String months;
    private Double income = 0.0;
    private Double outcome =0.0;


    public MonthlyStatisticsDto(String month) {
        this.months = month;
    }
}
