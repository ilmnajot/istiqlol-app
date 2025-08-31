package org.example.moliyaapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.moliyaapp.enums.Months;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class StudentCutAmountDto {

    private Long studentContractId;
    private Months month;
    private Double amount;
    private String reason;
    private List<String> imageUrls;
    private Long feeId;


    @Data
    public static class UpdateStudentCutAmountDto {
        private Months month;
        private Double amount;
        private String reason;
        private List<String> imageUrls;
        private Long feeId;

    }
}
