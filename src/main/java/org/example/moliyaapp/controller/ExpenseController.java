package org.example.moliyaapp.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.moliyaapp.dto.ApiResponse;
import org.example.moliyaapp.dto.ExpensesDto;
import org.example.moliyaapp.enums.*;
import org.example.moliyaapp.filter.*;
import org.example.moliyaapp.service.ExpensesService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/expense")
public class ExpenseController {
    private final ExpensesService expensesService;

    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER')")
    @PostMapping("/create")
    public HttpEntity<ApiResponse> create(@RequestBody ExpensesDto.CreateExpense dto) {
        ApiResponse apiResponse = this.expensesService.create(dto);
        return ResponseEntity.status(apiResponse.getStatus()).body(apiResponse);
    }

    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER','OWNER','PRINCIPAL')")
    @GetMapping("/getById")
    public ApiResponse getById(@RequestParam("id") Long id) {
        return this.expensesService.getById(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER','OWNER','PRINCIPAL')")
    @GetMapping("/getAll")
    public ApiResponse getAll(@RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
                              @RequestParam(value = "size", required = false, defaultValue = "20") Integer size,
                              @RequestParam(value = "keyword", required = false) String keyword,
                              @RequestParam(value = "categoryId", required = false) Long categoryId,
                              @RequestParam(value = "deleted", required = false) Boolean deleted) {
        ExpensesFilter filter = new ExpensesFilter();
        filter.setKeyword(keyword);
        filter.setCategoryId(categoryId);
        filter.setDeleted(deleted);
        return this.expensesService.getAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")), filter);
    }

    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER','OWNER','PRINCIPAL')")
    @GetMapping("/getAllDeletedExpenses")
    public ApiResponse getAllDeletedExpenses(
            @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "20") Integer size) {
        return this.expensesService.getAllDeletedExpenses(PageRequest.of(page, size));
    }

    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER')")
    @DeleteMapping("/delete")
    public HttpEntity<ApiResponse> delete(@RequestParam("id") Long id) {
        ApiResponse apiResponse = this.expensesService.delete(id);
        return ResponseEntity.status(apiResponse.getStatus()).body(apiResponse);
    }

    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER')")
    @PutMapping("/update-expenses/{id}")
    public HttpEntity<ApiResponse> updateExpense(
            @PathVariable(name = "id") Long id,
            @RequestBody ExpensesDto.UpdateExpense dto) {
        ApiResponse apiResponse = this.expensesService.updateExpense(id, dto);
        return ResponseEntity.status(apiResponse.getStatus()).body(apiResponse);
    }

    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER','OWNER','PRINCIPAL')")
    @GetMapping("/get-daily-expenses")
    public ApiResponse getDailyExpenses(
            @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "20") Integer size) {
        return this.expensesService.getDailyExpenses(PageRequest.of(page, size));
    }

    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER','OWNER','PRINCIPAL')")
    @GetMapping("/get-weekly-expenses")
    public ApiResponse getWeeklyExpenses(@RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
                                         @RequestParam(value = "size", required = false, defaultValue = "20") Integer size) {
        return this.expensesService.getWeeklyExpenses(PageRequest.of(page, size));
    }

    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER','OWNER','PRINCIPAL')")
    @GetMapping("/get-monthly-expenses")
    public ApiResponse getMonthlyExpenses(@RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
                                          @RequestParam(value = "size", required = false, defaultValue = "20") Integer size) {
        return this.expensesService.getMonthlyExpenses(PageRequest.of(page, size));
    }

    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER','OWNER','PRINCIPAL')")
    @GetMapping("/get-yearly-expenses")
    public ApiResponse getYearlyExpenses(@RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
                                         @RequestParam(value = "size", required = false, defaultValue = "20") Integer size) {
        return this.expensesService.getYearlyExpenses(PageRequest.of(page, size));
    }

    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER','OWNER','PRINCIPAL')")
    @GetMapping("/get-expenses-date-range")
    public ApiResponse getAllExpensesDateRange(@RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
                                               @RequestParam(value = "size", required = false, defaultValue = "20") Integer size,
                                               @RequestParam(name = "start", required = false) LocalDate start,
                                               @RequestParam(name = "end", required = false) LocalDate end) {
        return this.expensesService.getAllExpensesByDateRange(PageRequest.of(page, size), start, end);
    }

    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER','OWNER','PRINCIPAL')")
    @GetMapping("/get-all-by-category-id/{categoryId}")
    public ApiResponse getAllByCategoryId(@PathVariable(value = "categoryId") Long categoryId,
                                          @RequestParam(value = "page", defaultValue = "0") Integer page,
                                          @RequestParam(value = "size", defaultValue = "20") Integer size) {
        return this.expensesService.findAllByCategory(categoryId, PageRequest.of(page, size));
    }

    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER','OWNER','PRINCIPAL')")
    @DeleteMapping("/delete-expense-list")
    public HttpEntity<ApiResponse> deleteExpensesList(@RequestParam("ids") List<Long> ids) {
        ApiResponse apiResponse = this.expensesService.deleteExpensesList(ids);
        return ResponseEntity.status(apiResponse.getStatus()).body(apiResponse);
    }

    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER','OWNER','PRINCIPAL')")
    @GetMapping("/get-all-expenses-by-categoryStatus")
    public HttpEntity<ApiResponse> getAllByCategoryStatus(
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "deleted", required = false) Boolean deleted,
            @RequestParam(value = "categoryStatus", required = false) CategoryStatus status,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "20") int size) {
        ExpensesFilterWithStatus filter = new ExpensesFilterWithStatus();
        filter.setCategoryStatus(status);
        filter.setDeleted(deleted);
        filter.setCategoryId(categoryId);
        filter.setDate(date);
        filter.setKeyword(keyword);
        if (from != null) {
            filter.setFrom(from.atStartOfDay()); // 00:00:00
        }
        if (to != null) {
            filter.setTo(to.atTime(LocalTime.MAX)); // 23:59:59.999999999
        }
        ApiResponse apiResponse = this.expensesService.getAllByCategoryStatus(filter, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ResponseEntity.status(apiResponse.getStatus()).body(apiResponse);
    }

    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER','OWNER','PRINCIPAL')")
    @GetMapping("/download-all-expense-sheet")
    public void downloadExcel(
            @RequestParam(value = "transactionType") TransactionType transactionType,
            @RequestParam(value = "createdAtFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdAtFrom,
            @RequestParam(value = "createdAtTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdAtTo,
            @RequestParam(value = "deleted", required = false, defaultValue = "false") Boolean deleted,
            HttpServletResponse response
    ) throws IOException {
        ExpensesSheetFilter filter = new ExpensesSheetFilter();
        filter.setTransactionType(transactionType);
        if (createdAtFrom != null) {
            filter.setStartDate(createdAtFrom.atStartOfDay()); // 00:00:00
        }
        if (createdAtTo != null) {
            filter.setEndDate(createdAtTo.atTime(LocalTime.MAX)); // 23:59:59.999999999
        }
        filter.setDeleted(deleted);
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=xarajatlar.xlsx");
        this.expensesService.downloadExcel(filter, response.getOutputStream());
    }


    // Controller method
    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER','OWNER','PRINCIPAL')")
    @PostMapping(path = "/upload-sheet", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadExpenseSheet(
            @RequestParam("file") MultipartFile file
    ) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("Fayl tanlangan emas!");
            }

            if (!file.getOriginalFilename().endsWith(".xlsx") && !file.getOriginalFilename().endsWith(".xls")) {
                return ResponseEntity.badRequest().body("Faqat Excel fayllar qabul qilinadi (.xlsx, .xls)!");
            }

            int uploadedCount = this.expensesService.uploadExpenseSheet(file);
            return ResponseEntity.ok("Muvaffaqiyatli yuklandi! " + uploadedCount + " ta xarajat qo'shildi.");

        } catch (Exception e) {
            log.error("Excel fayl yuklashda xatolik: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Fayl yuklashda xatolik: " + e.getMessage());
        }
    }


    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER','OWNER','PRINCIPAL')")
    @GetMapping("/get-statistics-by-month")
    public ApiResponse getExpensesDataByMonth(@RequestParam(value = "transactionType", defaultValue = "INCOME") TransactionType transactionType,
                                              @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                              @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return this.expensesService.getExpensesDataByMonth(transactionType, from, to);
    }


}
