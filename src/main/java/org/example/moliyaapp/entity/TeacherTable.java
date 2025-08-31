package org.example.moliyaapp.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.moliyaapp.enums.Months;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "teacher_table")
@Builder
public class TeacherTable extends BaseEntity {

    private Integer workDaysOrLessons;
    private Integer workedDaysOrLessons;
    private Integer extraWorkedDaysOrLessons;
    private Integer missedWorkDaysOrLessons;

    @Enumerated(EnumType.STRING)
    private Months months;
    private String description;

    private Double monthlySalary;
    private Double amount;

    @ManyToOne(optional = false)
    @JoinColumn(name = "teacher_id")
    private User teacher;

    @ManyToOne
    @JoinColumn(name = "teacher_contract_id")
    private TeacherContract teacherContract;

    @ManyToOne
    @JoinColumn(name = "extra_lesson_price_id")
    private ExtraLessonPrice extraLessonPrice;

}
