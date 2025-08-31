package org.example.moliyaapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomDto {
    private Long id;
    private String name;
    private Integer number;
    private Integer floor;

    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean deleted;



    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateRoom {
        private String name;
        private Integer number;
        private Integer floor;
    }
}
