package org.example.moliyaapp.service;

import org.example.moliyaapp.entity.TeacherTable;
import org.example.moliyaapp.entity.User;

public interface EmployeeTabelSheet {

    void recordEmployeeTabel(TeacherTable teacherTable);
    void initializeSheet();
    void updateEmployeeTabel(TeacherTable teacherTable);
}
