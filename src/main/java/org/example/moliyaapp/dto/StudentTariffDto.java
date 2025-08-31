package org.example.moliyaapp.dto;

import lombok.*;
import org.example.moliyaapp.enums.TariffStatus;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class StudentTariffDto {
    private Long id;
    private String name;
    private Double amount;
    private TariffStatus tariffStatus; // Assuming TariffStatus is a String representation
    private Boolean isUsed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
    private Boolean deleted;

    @Data
    public static class TariffCreateDto {
        private String name;
        private Double amount;
        private TariffStatus tariffStatus; // Assuming TariffStatus is an enum
    }
    @Data
    public static class TariffUpdateDto {
        private String name;
        private Double amount;
        private TariffStatus tariffStatus; // Assuming TariffStatus is an enum

    }
}
