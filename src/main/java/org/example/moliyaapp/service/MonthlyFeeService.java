package org.example.moliyaapp.service;

import org.example.moliyaapp.dto.ApiResponse;
import org.example.moliyaapp.dto.MonthlyFeeDto;
import org.example.moliyaapp.dto.StudentCutAmountDto;
import org.example.moliyaapp.enums.*;
import org.example.moliyaapp.filter.MonthlyFeeFilter;
import org.example.moliyaapp.filter.MonthlyFeeFilterWithStatus;
import org.example.moliyaapp.filter.TransactionEmployeeFilter;
import org.example.moliyaapp.filter.TransactionSheetFilter;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.OutputStream;
import java.time.LocalDate;
import java.util.List;

@Component
public interface MonthlyFeeService {

    ApiResponse createStudent(MonthlyFeeDto.CreateMonthlyFee dto);

    ApiResponse createEmployee(MonthlyFeeDto.CreateMonthlyFee dto);

    ApiResponse getById(Long id);

    ApiResponse getAll(MonthlyFeeFilter filter, Pageable pageable);

    ApiResponse delete(Long id);

    ApiResponse getAllByStatus(PaymentStatus status, Pageable pageable);

    ApiResponse getAllByMonths(Months months, Pageable pageable);

    ApiResponse countByStatus();

    ApiResponse getAllAmountByTypeADay(TransactionType type,LocalDate date);

    ApiResponse getAllAmountByTypeAWeek(TransactionType type,LocalDate start, LocalDate end);

    ApiResponse getAllAmountByTypeAMonths(TransactionType type,LocalDate start, LocalDate end);

    ApiResponse getAllAmountByTypeAYear(TransactionType type,Integer year);

    ApiResponse updateBalance(Double amount, Long id, Months months, PaymentType paymentType, PaymentStatus paymentStatus);

    ApiResponse getGraphStatistics(Integer year);

    ApiResponse deleteMFList(List<Long> ids);

    ApiResponse getAllByCategoryStatus(MonthlyFeeFilterWithStatus status, Pageable pageable);

    ApiResponse getStatistics(PeriodType periodType, TransactionType transactionType, LocalDate startDate, LocalDate endDate, Integer year);

    ApiResponse getAllTransactionsStudent(Long studentContractId);

    ApiResponse getAllTransactionsEmployee(Long employeeId);

    ApiResponse cutStudentAmount(StudentCutAmountDto dto);

    void downloadExcel(TransactionSheetFilter filter, OutputStream outputStream);

    ApiResponse getAllTransactionsEmployeeId(Pageable pageable, TransactionEmployeeFilter filter);

    ApiResponse getAllAmountOverview(Integer year, LocalDate date);

    ApiResponse getHourlyAmountData(TransactionType type, LocalDate date, Integer year);

    ApiResponse getWeeklyChartData(TransactionType type, Integer year, LocalDate fromDate, LocalDate toDate);

    ApiResponse getMonthlyChartData(TransactionType type, Integer year, LocalDate fromDate, LocalDate toDate);

    ApiResponse getYearlyChartData(TransactionType type, Integer year);

    ApiResponse uploadImage(MultipartFile image);

    ApiResponse removeImage(String imageUrl);

    ApiResponse updateCutStudentAmount(StudentCutAmountDto dto, Long transactionId);
}
