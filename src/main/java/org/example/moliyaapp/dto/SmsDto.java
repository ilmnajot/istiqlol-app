package org.example.moliyaapp.dto;

import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SmsDto {
    private String phoneNumber;
    private String title;
    private String text;
    private Long studentContractId;
    private Long monthlyFeeId;


    @Data
    public static class SmsReportDto {
        private int year;
        private int month;
    }

    @Builder
    @Data
    public static class SmsHistoryDto {
        private String startDate;  // "2024-07-01"
        private String endDate;    // "2024-07-31"
        private Integer limit;     // 100
        private Integer offset;    // 0
        private String phoneNumber; // filter by phone
        private String status;     // filter by status
    }
}

