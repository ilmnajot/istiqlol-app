package org.example.moliyaapp.projection;

import lombok.*;
import org.example.moliyaapp.enums.Gender;
import org.example.moliyaapp.enums.Grade;
import org.example.moliyaapp.enums.StudentGrade;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@Builder
public class StudentsByGender {
    private Gender gender;
    private int count;
}
