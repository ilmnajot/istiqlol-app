package org.example.moliyaapp.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.moliyaapp.enums.SalaryType;

import java.time.LocalDate;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "teacher_contract")
@Builder
public class TeacherContract extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @Enumerated(EnumType.STRING)
    private SalaryType salaryType;

    private Double monthlySalaryOrPerLessonOrPerDay;

    private LocalDate startDate;

    private LocalDate endDate; // null means ongoing

    private Boolean active = Boolean.TRUE;

}
