package org.example.moliyaapp.service;

import org.example.moliyaapp.dto.ApiResponse;
import org.example.moliyaapp.dto.ReminderDto;
import org.example.moliyaapp.dto.StudentContractDto;
import org.example.moliyaapp.enums.Grade;
import org.example.moliyaapp.enums.Months;
import org.example.moliyaapp.enums.PeriodType;
import org.example.moliyaapp.enums.StudentGrade;
import org.example.moliyaapp.filter.ExpenseTransactionFilter;
import org.example.moliyaapp.filter.StudentContractFilter;
import org.example.moliyaapp.filter.StudentReminderFilter;
import org.example.moliyaapp.filter.TransactionFilter;
import org.example.moliyaapp.projection.MonthlyPaymentInfoDto;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.util.List;

public interface StudentContractService {
    ApiResponse createContract(StudentContractDto.CreateStudentContractDto dto);

    ApiResponse getStudentContract(Long id);

    ApiResponse getAllStudentContracts(int page, int size);

    ApiResponse deleteContractStudent(Long id);

    ApiResponse update(Long id, StudentContractDto.UpdateStudentContractDto dto);

    ApiResponse getStudentContractByDate(int page, int size, LocalDate fromDate, LocalDate toDate);

    ApiResponse getAllStudents(int page, int size, Boolean status);

    ApiResponse getDeletedStudents(int page, int size);

    ApiResponse filter(StudentContractFilter filter, Pageable pageable);

    void uploadExcel(MultipartFile file) throws IOException;

    void downloadExcel(StudentContractFilter filter, OutputStream outputStream);

    ApiResponse getAllStudentContractsList(StudentGrade grade);

    ApiResponse getDailyStudentContract();

    ApiResponse getWeeklyStudentContract();

    ApiResponse getYearlyStudentContract();

    ApiResponse getMonthlyStudentContract();

    ApiResponse getMonthlyStudentContractsByYearAndYear(int year, Months month);

    ApiResponse deleteStudents(List<Long> ids);

    ApiResponse getMonthlyContractStats(int month, int year);

    ApiResponse getContractStatistics(PeriodType periodType, Boolean status, LocalDate startDate, LocalDate endDate, Integer year);

    //    ApiResponse getByMonthName(Months month, Long studentId, Pageable pageable);
    ApiResponse getData(String academicYear, Boolean status);

    ApiResponse getDataWithTariff(String academicYear);

    ApiResponse payForStudent(Long studentContractId, StudentContractDto.StudentPaymentDto dto);

    ApiResponse getPaymentInfoByContractId(Long contractId, String academicYear);

    List<MonthlyPaymentInfoDto> getAllMonthsPaymentInfo(Long contractId);

    ApiResponse payStudentTuition(Long studentContractId, StudentContractDto.StudentPaymentDto dto);

    ApiResponse terminateContract(Long id);

    ApiResponse addReminder(Long studentContractId, ReminderDto.ReminderCreateAndUpdateDto reminderDto);

    ApiResponse getStudentReminders(Long studentContractId);

    ApiResponse getInfo(String academicYear, Boolean status, LocalDate from, LocalDate to);

    ApiResponse transferStudents(List<Long> studentIds, StudentGrade studentGrade, Grade grade, String academicYear, Long tariffId, LocalDate contractStartDate, LocalDate contractEndDate);

    ApiResponse updateStudentTuition(Long transactionId, StudentContractDto.StudentPaymentDto dto);

    void  reminderFilter(StudentReminderFilter filter, OutputStream outputStream);

    ApiResponse transactionFilter(TransactionFilter filter, Pageable pageable);

    ApiResponse updateStudentTariff(Long studentContractId, Long tariffId, String reason);

    ApiResponse getAllTariffHistory(Long studentContractId);

    ApiResponse expenseTransactionFilter(ExpenseTransactionFilter filter, Pageable pageable);
}
