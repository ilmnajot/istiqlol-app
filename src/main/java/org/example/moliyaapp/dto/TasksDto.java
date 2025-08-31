package org.example.moliyaapp.dto;

import lombok.*;
import org.example.moliyaapp.enums.TaskPriority;
import org.example.moliyaapp.enums.TaskType;
import org.example.moliyaapp.enums.TasksStatus;

import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TasksDto {


    private Long id;
    private String name;
    private String description;
    private LocalDateTime assignedDate;
    private Long employeeId;
    private TaskPriority taskPriority;
    private TaskType taskType;
    private TasksStatus tasksStatus;

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
    public static class TasksCreateDto {

        private String name;
        private String description;
        private LocalDateTime assignedDate;
        private TaskPriority taskPriority;
        private TaskType taskType;
        private TasksStatus tasksStatus;
        private boolean deleted;

    }
    @Setter
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class TasksUpdateDto {

        private String name;
        private String description;
        private LocalDateTime assignedDate;
        private TaskPriority taskPriority;
        private Long employeeId;
        private TaskType taskType;
        private TasksStatus tasksStatus;
        private boolean deleted;

    }

}
