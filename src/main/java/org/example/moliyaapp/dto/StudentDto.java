package org.example.moliyaapp.dto;

import lombok.Data;
import org.example.moliyaapp.enums.StudentGrade;

import java.time.LocalDateTime;

public class StudentDto {


    @Data
    public static class Request {

        private String fullName;
        private String BCN; // Birth Certificate Number (Tug'ilganlik haqidagi guvohnoma)
        private StudentGrade grade;
    }

    @Data
    public static class Response {
        private Long id;
        private String fullName;
        private String BCN; // Birth Certificate Number (Tug'ilganlik haqidagi guvohnoma)
        private boolean graduated; //active
        private StudentGrade grade;
        private Long groupId; //7-A, 8-B
        private Long studentContractId;

        private Long createdBy;
        private Long updatedBy;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private Boolean deleted;

    }

    @Data
    public static class Update {
        private String fullName;
        private String BCN; // Birth Certificate Number (Tug'ilganlik haqidagi guvohnoma)
        private boolean graduated;
        private StudentGrade grade;
        private Long groupId; //7-A, 8-B
    }

}
