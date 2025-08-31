package org.example.moliyaapp.dto;

import lombok.*;
import org.example.moliyaapp.enums.Months;

import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TeacherTableDto {

    private Long id;
    private UserDto userDto;

    private Integer workDaysOrLessons;
    private Integer workedDaysOrLessons;
    private Integer extraWorkedDaysOrLessons;
    private Integer missedWorkDaysOrLessons;

//    private Integer allLessonsCountsCovered;
    private Months months;
    private String description;
    private Long teacherContract;//new
    private Double finalAmount;
    private ExtraLessonPriceDto extraLessonPriceDto;

//    //new things to go
//    private int workDays;
    private Double cardAmount;
    private Double cashAmount;
    private Double remainingBalance;
    private Double bonusAmount;

    private Double extraAmount;
    private Double cuttingAmount;
    private Double salary;

//    private Double amountForExtraLesson;


    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
    private Boolean deleted;

    @Data
    public static class CreateTeacherTableDto {

        private Integer workDaysOrLessons;
        private Integer workedDaysOrLessons;
        private Integer extraWorkedDaysOrLessons;
        private Double salary;
//        private Integer missedWorkDaysOrLessons;
        private Months months;
        private String description;
        private Long teacherId;
        private Long extraLessonPriceId;

    }

}
