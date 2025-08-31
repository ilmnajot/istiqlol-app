package org.example.moliyaapp.dto;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;

@Setter
@Getter
public class
GroupDto {


    @Data
    public static class Request {
        private String name;
        private boolean active;

    }


    @Data
    public static class Response {
        private Long id;
        private String name;
        private boolean active;

        private Long createdBy;
        private Long updatedBy;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private Boolean deleted;

    }

    @Data
    public static class Update {
        private String name;
        private boolean active;
    }
}
