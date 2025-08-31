package org.example.moliyaapp.service;

import org.example.moliyaapp.dto.ApiResponse;
import org.example.moliyaapp.dto.StudentBonusDto;
import org.example.moliyaapp.enums.BonusType;

import java.time.LocalDate;

public interface StudentBonusService {
    ApiResponse awardBonusToStudent(StudentBonusDto.createStudentBonus dto);

    ApiResponse getStudentBonusById(Long id);

    ApiResponse getAllStudentBonus(int page, int size);

    ApiResponse deleteStudentBonus(Long id);

    ApiResponse updateStudentBonus(Long id, StudentBonusDto.UpdateStudentBonus dto);

    ApiResponse getAllByBonusType(BonusType bonusType, int page, int size);

    ApiResponse getStudentBonusByDateRange(int page, int size, LocalDate fromDate, LocalDate toDate);
}