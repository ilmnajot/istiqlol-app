package org.example.moliyaapp.service;

import org.example.moliyaapp.entity.User;

public interface EmployeeGoogleSheet {

    void recordEmployee(User user);
    void initializeSheet();
    void updateEmployee(User user);
}
