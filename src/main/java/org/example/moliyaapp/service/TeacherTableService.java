package org.example.moliyaapp.service;

import jakarta.servlet.ServletOutputStream;
import org.example.moliyaapp.dto.ApiResponse;
import org.example.moliyaapp.dto.StudentContractDto;
import org.example.moliyaapp.dto.TeacherTableDto;
import org.example.moliyaapp.enums.Months;
import org.example.moliyaapp.enums.Role;
import org.example.moliyaapp.filter.TeacherTabelSheetFilter;
import org.example.moliyaapp.filter.TeacherTableFilter;
import org.springframework.data.domain.Pageable;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.util.List;

public interface TeacherTableService {

    ApiResponse addTeacherTable(TeacherTableDto.CreateTeacherTableDto dto);

    ApiResponse deleteTeacherTable(Long id);

    ApiResponse getTeacherTableById(Long id);

    ApiResponse getAllTeacherTableList();

    ApiResponse getAllTeacherTablePage(Pageable pageable);

    ApiResponse updateTeacherTable(Long id, TeacherTableDto.CreateTeacherTableDto dto);

    ApiResponse getByTeacherIdAndMonth(Long teacherId, Months month);

    ApiResponse getAllTeacherTableByMonth(Months month, Integer year);

    ApiResponse filter(Pageable pageable, TeacherTableFilter filter);

    ApiResponse getAllUsersByRole(Role role, Pageable pageable, Boolean otherRoles);

    ApiResponse getAllByMonth(Months months, Pageable pageable);

    ApiResponse searchByName(String keyword, Pageable pageable);

    ApiResponse deleteConctracts(List<Long> ids);

    ApiResponse payForCash(Long tableId, Double amount, Double bonus, String comment);

    ApiResponse payForCard(List<Long> employeeIds, Months months, Double amount);

    void downloadExcel(TeacherTabelSheetFilter filter, OutputStream outputStream);

    ApiResponse payAdvanceForCash(Long employeeContractId, Double amount, Months month, String comment, Boolean isAdvanced);

    ApiResponse updateEmployeeTuition(Long transactionId, StudentContractDto.EmployeePaymentDto dto);

    ApiResponse updateEmployeeBonusTuition(Long bonusId, StudentContractDto.EmployeeBonusPaymentDto dto);

    ApiResponse deleteTables(List<Long> ids);
}
