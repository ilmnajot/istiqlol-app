package org.example.moliyaapp.dto;

import lombok.*;
import org.example.moliyaapp.enums.CategoryStatus;
import org.example.moliyaapp.enums.TransactionType;

import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CategoryDto {

    private Long id;
    private String name;
    private CategoryStatus categoryStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
    private Boolean deleted;

    @Data
    public static class CategoryCreateDto {

        private String name;
        private CategoryStatus categoryStatus;

    }
}
