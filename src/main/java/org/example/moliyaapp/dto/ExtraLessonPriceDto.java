package org.example.moliyaapp.dto;

import lombok.*;

import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ExtraLessonPriceDto {

    private Long id;
    private String name;
    private Double fixedAmount;
    private Boolean isUsed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
    private Boolean deleted;

    @Data
    public static class CreatedAndUpdateDto {
        private String name;
        private Double fixedAmount;
    }
}
