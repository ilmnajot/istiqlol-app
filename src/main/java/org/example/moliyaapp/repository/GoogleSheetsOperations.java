package org.example.moliyaapp.repository;

import org.example.moliyaapp.entity.Company;
import org.example.moliyaapp.entity.StudentContract;
import org.example.moliyaapp.entity.StudentTariff;

// Interface for Google Sheets operations
public interface GoogleSheetsOperations {
    void recordStudentContract(StudentContract contract, StudentTariff tariff, Company company);

    void initializeSheet();
    void updateStudentContractRow(StudentContract contract);

}