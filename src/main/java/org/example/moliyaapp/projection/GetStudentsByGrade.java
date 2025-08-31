package org.example.moliyaapp.projection;

import org.example.moliyaapp.enums.Grade;
import org.example.moliyaapp.enums.StudentGrade;
import org.example.moliyaapp.enums.StudyLanguage;

public interface GetStudentsByGrade {

    StudentGrade getStudentGrade();

    Grade getGrade();

    int getMaleCount();

    int getFemaleCount();

    int getOldCount();

    int getNewCount();

    String getStudyLanguage();


}
