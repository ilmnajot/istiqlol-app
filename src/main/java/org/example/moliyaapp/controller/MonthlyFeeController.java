package org.example.moliyaapp.controller;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.moliyaapp.dto.ApiResponse;
import org.example.moliyaapp.dto.MonthlyFeeDto;
import org.example.moliyaapp.dto.StudentCutAmountDto;
import org.example.moliyaapp.enums.*;
import org.example.moliyaapp.filter.MonthlyFeeFilter;
import org.example.moliyaapp.filter.MonthlyFeeFilterWithStatus;
import org.example.moliyaapp.filter.TransactionEmployeeFilter;
import org.example.moliyaapp.filter.TransactionSheetFilter;
import org.example.moliyaapp.service.MonthlyFeeService;
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
@RequestMapping("/monthlyFee")
public class MonthlyFeeController {
    private final MonthlyFeeService monthlyFeeService;


    @Hidden
    @PostMapping("/pay-for-student")
    public HttpEntity<ApiResponse> createStudent(@RequestBody MonthlyFeeDto.CreateMonthlyFee dto) {
        ApiResponse apiResponse = this.monthlyFeeService.createStudent(dto);
        return ResponseEntity.status(apiResponse.getStatus()).body(apiResponse);
    }

    @Hidden
    @PostMapping("/pay-for-employee")
    public HttpEntity<ApiResponse> createEmployee(@RequestBody MonthlyFeeDto.CreateMonthlyFee dto) {
        ApiResponse apiResponse = this.monthlyFeeService.createEmployee(dto);
        return ResponseEntity.status(apiResponse.getStatus()).body(apiResponse);
    }

    @PreAuthorize("hasAnyRole('DEVELOPER', 'OWNER', 'PRINCIPAL', 'ADMIN', 'HR', 'EDUCATIONAL_DEPARTMENT')")
    @GetMapping("/get-monthlyFee-by-id")
    public ApiResponse getEmployeeMonthlyFeeById(@RequestParam("id") Long id) {
        return this.monthlyFeeService.getById(id);
    }

    @PreAuthorize("hasAnyRole('DEVELOPER', 'OWNER', 'PRINCIPAL', 'ADMIN', 'HR', 'EDUCATIONAL_DEPARTMENT')")
    @GetMapping("/getAll")
    public ApiResponse getAll(
            @RequestParam(value = "deleted", required = false) Boolean deleted,
            @RequestParam(value = "categoryName", required = false) String categoryName,
            @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "20") Integer size) {
        MonthlyFeeFilter filter = new MonthlyFeeFilter();
        filter.setDeleted(deleted);
        filter.setCategoryName(categoryName);
        return this.monthlyFeeService.getAll(filter, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @PreAuthorize("hasAnyRole('DEVELOPER','ADMIN')")
    @DeleteMapping("/delete")
    public HttpEntity<ApiResponse> delete(@RequestParam("id") Long id) {
        ApiResponse apiResponse = this.monthlyFeeService.delete(id);
        return ResponseEntity.status(apiResponse.getStatus()).body(apiResponse);
    }

    @PreAuthorize("hasAnyRole('DEVELOPER','ADMIN','PRINCIPAL','OWNER','')")
    @GetMapping("/getAllByStatus")
    public ApiResponse getAllByStatus(@RequestParam("status") PaymentStatus status,
                                      @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
                                      @RequestParam(value = "size", required = false, defaultValue = "20") Integer size) {
        return this.monthlyFeeService.getAllByStatus(status, PageRequest.of(page, size));
    }

    @PreAuthorize("hasAnyRole('DEVELOPER','ADMIN','PRINCIPAL','OWNER','')")
    @GetMapping("/getAllByMonths")
    public ApiResponse getAllByMonths(@RequestParam("months") Months months,
                                      @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
                                      @RequestParam(value = "size", required = false, defaultValue = "20") Integer size) {
        return this.monthlyFeeService.getAllByMonths(months, PageRequest.of(page, size));
    }

    @Hidden
    @GetMapping("/countByStatus")
    public ApiResponse countByStatus() {
        return this.monthlyFeeService.countByStatus();
    }

    // Controllers for Chart Data
    @PreAuthorize("hasAnyRole('DEVELOPER','ADMIN','PRINCIPAL','OWNER')")
    @GetMapping("/getWeeklyChartData")
    public ApiResponse getWeeklyChartData(
            @RequestParam("type") TransactionType type,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return this.monthlyFeeService.getWeeklyChartData(type, year, fromDate, toDate);
    }

    @PreAuthorize("hasAnyRole('DEVELOPER','ADMIN','PRINCIPAL','OWNER')")
    @GetMapping("/getMonthlyChartData")
    public ApiResponse getMonthlyChartData(
            @RequestParam("type") TransactionType type,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return this.monthlyFeeService.getMonthlyChartData(type, year, from, to);
    }

    @PreAuthorize("hasAnyRole('DEVELOPER','ADMIN','PRINCIPAL','OWNER')")
    @GetMapping("/getYearlyChartData")
    public ApiResponse getYearlyChartData(
            @RequestParam("type") TransactionType type,
            @RequestParam(value = "year", required = false) Integer year) {
        return this.monthlyFeeService.getYearlyChartData(type, year);
    }

    @PreAuthorize("hasAnyRole('DEVELOPER','ADMIN','PRINCIPAL','OWNER')")
    @GetMapping("/get-hourly-amount-data")
    public ApiResponse getHourlyAmountData(@RequestParam(value = "type") TransactionType type,
                                           @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                           @RequestParam(value = "year", required = false, defaultValue = "2025") Integer year) {
        return this.monthlyFeeService.getHourlyAmountData(type, date, year);
    }

    @PreAuthorize("hasAnyRole('DEVELOPER','ADMIN','PRINCIPAL','OWNER')")
    @GetMapping("/getAllAmountOverview")
    public ApiResponse getAllAmountOverview(@RequestParam("year") Integer year,
                                            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return this.monthlyFeeService.getAllAmountOverview(year, date);
    }


    @PreAuthorize("hasAnyRole('DEVELOPER','ADMIN','PRINCIPAL','OWNER')")
    @GetMapping("/getAllAmountByTypeADay")
    public ApiResponse getAllAmountByTypeADay(@RequestParam("type") TransactionType type,
                                              @RequestParam(value = "date", required = false, defaultValue = "2025-08-08") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return this.monthlyFeeService.getAllAmountByTypeADay(type, date);
    }

    @PreAuthorize("hasAnyRole('DEVELOPER','ADMIN','PRINCIPAL','OWNER')")
    @GetMapping("/getAllAmountByTypeAWeek")
    public ApiResponse getAllAmountByTypeAWeek(@RequestParam("type") TransactionType type,
                                               @RequestParam(value = "startDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
                                               @RequestParam(value = "endDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return this.monthlyFeeService.getAllAmountByTypeAWeek(type, startDate, endDate);
    }

    @PreAuthorize("hasAnyRole('DEVELOPER','ADMIN','PRINCIPAL','OWNER')")
    @GetMapping("/getAllAmountByTypeAMonths")
    public ApiResponse getAllAmountByTypeAMonths(@RequestParam("type") TransactionType type,
                                                 @RequestParam(value = "startDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
                                                 @RequestParam(value = "endDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return this.monthlyFeeService.getAllAmountByTypeAMonths(type, startDate, endDate);
    }

    @PreAuthorize("hasAnyRole('DEVELOPER','ADMIN','PRINCIPAL','OWNER')")
    @GetMapping("/getAllAmountByTypeAYear")
    public ApiResponse getAllAmountByTypeAYear(@RequestParam("type") TransactionType type,
                                               @RequestParam(value = "year", required = false) Integer year) {
        return this.monthlyFeeService.getAllAmountByTypeAYear(type, year);
    }


    @Hidden
    @PutMapping("/update-balance/{id}")
    public ApiResponse updateBalance(@RequestParam Double amount,
                                     @PathVariable(name = "id") Long id,
                                     @RequestParam Months months,
                                     @RequestParam PaymentType paymentType,
                                     @RequestParam PaymentStatus paymentStatus) {
        return this.monthlyFeeService.updateBalance(amount, id, months, paymentType, paymentStatus);
    }

    @GetMapping("/get-graph-statistics")
    public ApiResponse getGraphStatistics(@RequestParam(value = "year") Integer year) {
        return this.monthlyFeeService.getGraphStatistics(year);
    }

    @PreAuthorize("hasAnyRole('DEVELOPER','ADMIN','PRINCIPAL','OWNER')")
    @DeleteMapping("/delete-monthlyfee-list")
    public HttpEntity<ApiResponse> deleteMFList(@RequestParam("ids") List<Long> ids) {
        ApiResponse apiResponse = this.monthlyFeeService.deleteMFList(ids);
        return ResponseEntity.status(apiResponse.getStatus()).body(apiResponse);
    }

    @PreAuthorize("hasAnyRole('DEVELOPER','ADMIN','PRINCIPAL','OWNER')")
    @GetMapping("/get-all-monthlyFee-by-categoryStatus")
    public HttpEntity<ApiResponse> getAllByCategoryStatus(@RequestParam(value = "deleted", required = false) Boolean deleted,
                                                          @RequestParam(value = "keyword", required = false) String keyword,
                                                          @RequestParam(value = "categoryStatus", required = false) CategoryStatus status,
                                                          @RequestParam(value = "months", required = false) Months months,
                                                          @RequestParam(value = "paymentStatus", required = false) PaymentStatus paymentStatus,
                                                          @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                                          @RequestParam(value = "page", required = false, defaultValue = "0") int page,
                                                          @RequestParam(value = "size", required = false, defaultValue = "20") int size) {
        MonthlyFeeFilterWithStatus filter = new MonthlyFeeFilterWithStatus();
        filter.setCategoryStatus(status);
        filter.setCreatedAt(date);
        filter.setDeleted(deleted);
        filter.setMonths(months);
        filter.setStatus(paymentStatus);
        filter.setKeyword(keyword);
        ApiResponse apiResponse = this.monthlyFeeService.getAllByCategoryStatus(filter, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ResponseEntity.status(apiResponse.getStatus()).body(apiResponse);

    }

    @PreAuthorize("hasAnyRole('DEVELOPER','ADMIN','PRINCIPAL','OWNER')")
    @GetMapping("/get-statistics")
    public ApiResponse getStatistics(@RequestParam("periodType") PeriodType periodType,
                                     @RequestParam(value = "transactionType", required = false) TransactionType transactionType,
                                     @RequestParam(value = "startDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
                                     @RequestParam(value = "endDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
                                     @RequestParam(value = "year", required = false) Integer year
    ) {
        return this.monthlyFeeService.getStatistics(periodType, transactionType, startDate, endDate, year);
    }

    @PreAuthorize("hasAnyRole('DEVELOPER','ADMIN','PRINCIPAL','OWNER','RECEPTION','CASHIER')")
    @GetMapping("/get-student-all-transactions/{studentContractId}")
    public ApiResponse getAllTransactionsStudent(@PathVariable(value = "studentContractId") Long studentContractId) {
        return this.monthlyFeeService.getAllTransactionsStudent(studentContractId);
    }

    @PreAuthorize("hasAnyRole('DEVELOPER','ADMIN','PRINCIPAL','OWNER')")
    @GetMapping("/get-employee-all-transactions/{employeeId}")
    public ApiResponse getAllTransactionsEmployee(@PathVariable(value = "employeeId") Long employeeId) {
        return this.monthlyFeeService.getAllTransactionsEmployee(employeeId);

    }

    @PreAuthorize("hasAnyRole('DEVELOPER','ADMIN','PRINCIPAL','OWNER')")
    @GetMapping("/get-employee-transactions")
    public ApiResponse getAllTransactionsEmployee(
            @RequestParam(value = "amountType") String amountType,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "employeeName", required = false) String employeeName,
            @RequestParam(value = "month", required = false) Months month,
            @RequestParam(value = "type", required = false) TransactionType type,
            @RequestParam(value = "paymentType", required = false) PaymentType paymentType,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "20") int size) {
        TransactionEmployeeFilter filter = new TransactionEmployeeFilter();
        filter.setKeyword(keyword);
        filter.setAmountType(amountType);
        filter.setType(type);
        filter.setMonth(month);
        filter.setDeleted(false);
        filter.setPaymentType(paymentType);
        filter.setEmployeeName(employeeName);
        filter.setFrom(from);
        filter.setTo(to);
        return this.monthlyFeeService.getAllTransactionsEmployeeId(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")), filter);
    }

    @PreAuthorize("hasAnyRole('DEVELOPER','ADMIN')")
    @PostMapping("/cut-student-amount")
    public HttpEntity<ApiResponse> cutStudentAmount(@RequestBody StudentCutAmountDto dto) {
        ApiResponse apiResponse = this.monthlyFeeService.cutStudentAmount(dto);
        return ResponseEntity.status(apiResponse.getStatus()).body(apiResponse);

    }

    @PreAuthorize("hasAnyRole('DEVELOPER','ADMIN')")
    @PutMapping("/update-cut-student-amount/{studentContractId}")
    public HttpEntity<ApiResponse> updateCutStudentAmount(@PathVariable(value = "studentContractId") Long studentContractId,
                                                          @RequestBody StudentCutAmountDto dto) {
        ApiResponse apiResponse = this.monthlyFeeService.updateCutStudentAmount(dto, studentContractId);
        return ResponseEntity.status(apiResponse.getStatus()).body(apiResponse);

    }

    @PreAuthorize("hasAnyRole('DEVELOPER','ADMIN')")
    @PostMapping(value = "/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public HttpEntity<ApiResponse> uploadImage(@RequestParam(value = "image") MultipartFile image) {
        ApiResponse apiResponse = this.monthlyFeeService.uploadImage(image);
        return ResponseEntity.status(apiResponse.getStatus()).body(apiResponse);

    }

    @PreAuthorize("hasAnyRole('DEVELOPER','ADMIN')")
    @DeleteMapping("/delete-image")
    public HttpEntity<ApiResponse> removeImage(@RequestParam(value = "imageUrl") String imageUrl) {
        ApiResponse apiResponse = this.monthlyFeeService.removeImage(imageUrl);
        return ResponseEntity.status(apiResponse.getStatus()).body(apiResponse);

    }

    @PreAuthorize("hasAnyRole('DEVELOPER','ADMIN','PRINCIPAL','OWNER')")
    @GetMapping("/download-transaction-sheet")
    public void downloadExcel(
            @RequestParam(value = "transactionType") TransactionType transactionType,
            @RequestParam(value = "createdAtFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdAtFrom,
            @RequestParam(value = "createdAtTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdAtTo,
            @RequestParam(value = "paymentType", required = false, defaultValue = "NAQD") PaymentType paymentType,
            @RequestParam(value = "deleted", required = false, defaultValue = "false") Boolean deleted,
            HttpServletResponse response
    ) throws IOException {
        TransactionSheetFilter filter = new TransactionSheetFilter();
        filter.setTransactionType(transactionType);
        filter.setPaymentType(paymentType);
        if (createdAtFrom != null) {
            filter.setStartDate(createdAtFrom.atStartOfDay()); // 00:00:00
        }
        if (createdAtTo != null) {
            filter.setEndDate(createdAtTo.atTime(LocalTime.MAX)); // 23:59:59.999999999
        }
        filter.setDeleted(deleted);
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=transaksiyalar.xlsx");
        this.monthlyFeeService.downloadExcel(filter, response.getOutputStream());
    }

}

