package org.example.moliyaapp.projection;

import org.example.moliyaapp.enums.ClientType;
import org.example.moliyaapp.enums.Gender;
import org.example.moliyaapp.enums.Grade;
import org.example.moliyaapp.enums.StudentGrade;
import lombok.*;
import jakarta.persistence.Entity;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GetStudentsByGradeDto {

    private StudentGrade studentGrade;
    private Grade getGrade;
    private int maleCount;
    private int femaleCount;
    private int oldCount;
    private int newCount;
    private String studyLanguage;


}
