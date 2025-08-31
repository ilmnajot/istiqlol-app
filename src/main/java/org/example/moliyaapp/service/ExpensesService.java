package org.example.moliyaapp.service;

import jakarta.servlet.ServletOutputStream;
import org.example.moliyaapp.dto.ApiResponse;
import org.example.moliyaapp.dto.ExpensesDto;
import org.example.moliyaapp.enums.TransactionType;
import org.example.moliyaapp.filter.ExpensesFilter;
import org.example.moliyaapp.filter.ExpensesFilterWithStatus;
import org.example.moliyaapp.filter.ExpensesSheetFilter;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.util.List;

@Component
public interface ExpensesService {
    ApiResponse create(ExpensesDto.CreateExpense dto);

    ApiResponse getById(Long id);

    ApiResponse getAll(Pageable pageable, ExpensesFilter deleted);

    ApiResponse delete(Long id);

    ApiResponse getAllDeletedExpenses(Pageable pageable);

    ApiResponse updateExpense(Long id, ExpensesDto.UpdateExpense dto);

    ApiResponse getDailyExpenses(Pageable pageable);

    ApiResponse getWeeklyExpenses(Pageable pageable);

    ApiResponse getMonthlyExpenses(Pageable pageable);

    ApiResponse getYearlyExpenses(Pageable pageable);

    ApiResponse getAllExpensesByDateRange(Pageable pageable, LocalDate start, LocalDate end);

    ApiResponse findAllByCategory(Long categoryId, Pageable pageable);

    ApiResponse deleteExpensesList(List<Long> ids);

    ApiResponse getAllByCategoryStatus(ExpensesFilterWithStatus status, Pageable pageable);

    void downloadExcel(ExpensesSheetFilter filter, OutputStream outputStream);

    int uploadExpenseSheet(MultipartFile file);

    ApiResponse getExpensesDataByMonth(TransactionType transactionType, LocalDate from, LocalDate to);
}
