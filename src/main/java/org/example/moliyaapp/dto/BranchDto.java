package org.example.moliyaapp.dto;

import jakarta.persistence.Entity;
import lombok.*;

import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BranchDto {

    private Long id;
    private String branchName;
    private String address;
    private boolean active;
    private Long companyId;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean deleted;

    @Setter
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class BranchDtoCreate {
        private String branchName;
        private String address;
        private boolean active;
        private Long companyId;
    }
}
