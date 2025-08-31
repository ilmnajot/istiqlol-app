package org.example.moliyaapp.service;

import org.example.moliyaapp.entity.TeacherContract;
import org.example.moliyaapp.entity.User;

public interface EmployeeContractGoogleSheet {

    void recordEmployeeContract(TeacherContract user);
    void initializeSheet();
    void updateEmployeeContract(TeacherContract user);
}
