package org.example.moliyaapp.controller;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.example.moliyaapp.dto.ApiResponse;
import org.example.moliyaapp.dto.ReminderDto;
import org.example.moliyaapp.dto.StudentContractDto;
import org.example.moliyaapp.enums.*;
import org.example.moliyaapp.filter.ExpenseTransactionFilter;
import org.example.moliyaapp.filter.StudentContractFilter;
import org.example.moliyaapp.filter.StudentReminderFilter;
import org.example.moliyaapp.filter.TransactionFilter;
import org.example.moliyaapp.projection.MonthlyPaymentInfoDto;
import org.example.moliyaapp.service.StudentContractService;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/student_contracts")
public class StudentContractController {

    private final StudentContractService studentContractService;

    public StudentContractController(StudentContractService studentContractService) {
        this.studentContractService = studentContractService;
    }

    @PreAuthorize("hasAnyRole('DEVELOPER', 'RECEPTION')")
    @PostMapping("/make-student-contract")
    public HttpEntity<ApiResponse> addStudentContract(@RequestBody StudentContractDto.CreateStudentContractDto dto) {
        ApiResponse apiResponse = this.studentContractService.createContract(dto);
        return ResponseEntity.status(apiResponse.getStatus()).body(apiResponse);
    }

    @PreAuthorize("hasAnyRole('OWNER','PRINCIPAL','DEVELOPER','ADMIN','RECEPTION', 'STUDENT_CONTRACT_MAKER')")
    @GetMapping("/get-student-contract/{id}")
    public ApiResponse getStudentContract(@PathVariable(name = "id") Long id) {
        return this.studentContractService.getStudentContract(id);
    }

    @PreAuthorize("hasAnyRole('OWNER','PRINCIPAL','DEVELOPER','ADMIN','RECEPTION', 'STUDENT_CONTRACT_MAKER')")
    @GetMapping("/get-all-student-contracts")
    public ApiResponse getAllStudentContracts(@RequestParam(name = "page", defaultValue = "0") int page,
                                              @RequestParam(name = "size", defaultValue = "20") int size) {
        return this.studentContractService.getAllStudentContracts(page, size);
    }

    @PreAuthorize("hasAnyRole('OWNER','PRINCIPAL','DEVELOPER','ADMIN','RECEPTION', 'CASHIER')")
    @GetMapping("/get-all-student-contracts-list")
    public ApiResponse getAllStudentContractsList(@RequestParam(value = "grade", required = false) StudentGrade grade) {
        return this.studentContractService.getAllStudentContractsList(grade);
    }

    @PreAuthorize("hasAnyRole('DEVELOPER', 'RECEPTION', 'STUDENT_CONTRACT_MAKER')")
    @DeleteMapping("/delete-student-contract/{id}")
    public ApiResponse deleteStudentContract(@PathVariable(name = "id") Long id) {
        return this.studentContractService.deleteContractStudent(id);
    }

    @PreAuthorize("hasAnyRole('DEVELOPER', 'RECEPTION', 'STUDENT_CONTRACT_MAKER')")
    @PutMapping("/update-student-contract/{id}")
    public ApiResponse updateStudentContract(@PathVariable(name = "id") Long id,
                                             @RequestBody StudentContractDto.UpdateStudentContractDto dto) {
        return this.studentContractService.update(id, dto);
    }

    @PreAuthorize("hasAnyRole('OWNER','PRINCIPAL','DEVELOPER','ADMIN','RECEPTION', 'STUDENT_CONTRACT_MAKER')")
    @GetMapping("/get-contracts-by-date")
    public ApiResponse getStudentContractByDate(@RequestParam(name = "page", defaultValue = "0") int page,
                                                @RequestParam(name = "size", defaultValue = "20") int size,
                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return this.studentContractService.getStudentContractByDate(page, size, fromDate, toDate);
    }

    @PreAuthorize("hasAnyRole('OWNER','PRINCIPAL','DEVELOPER','ADMIN','RECEPTION', 'STUDENT_CONTRACT_MAKER')")
    @GetMapping("/get-numberOf-daily-student-contracts")
    public ApiResponse getDailyStudentContract() {
        return this.studentContractService.getDailyStudentContract();
    }

    @PreAuthorize("hasAnyRole('OWNER','PRINCIPAL','DEVELOPER','ADMIN','RECEPTION', 'STUDENT_CONTRACT_MAKER')")
    @GetMapping("/get-numberOf-weekly-student-contracts")
    public ApiResponse getWeeklyStudentContracts() {
        return this.studentContractService.getWeeklyStudentContract();
    }

    @PreAuthorize("hasAnyRole('OWNER','PRINCIPAL','DEVELOPER','ADMIN','RECEPTION', 'STUDENT_CONTRACT_MAKER')")
    @GetMapping("/get-numberOf-monthly-student-contracts")
    public ApiResponse getMonthlyStudentContracts() {
        return this.studentContractService.getMonthlyStudentContract();
    }

    @PreAuthorize("hasAnyRole('OWNER','PRINCIPAL','DEVELOPER','ADMIN','RECEPTION', 'STUDENT_CONTRACT_MAKER')")
    @GetMapping("/get-numberOf-monthly-student-contracts-custom-month-and-year")
    public ApiResponse getMonthlyStudentContractsByYearAndYear(@RequestParam(value = "year") int year,
                                                               @RequestParam(value = "month") Months month) {
        return this.studentContractService.getMonthlyStudentContractsByYearAndYear(year, month);
    }

    @PreAuthorize("hasAnyRole('OWNER','PRINCIPAL','DEVELOPER','ADMIN','RECEPTION', 'STUDENT_CONTRACT_MAKER')")
    @GetMapping("/get-numberOf-yearly-student-contracts")
    public ApiResponse getYearlyStudentContracts() {
        return this.studentContractService.getYearlyStudentContract();
    }

    @PreAuthorize("hasAnyRole('OWNER','PRINCIPAL','DEVELOPER','ADMIN','RECEPTION', 'CASHIER')")
    @GetMapping("/get-students-contracts-by-status")
    public ApiResponse getAllStudentsByStatus(@RequestParam(name = "page", defaultValue = "0") int page,
                                              @RequestParam(name = "size", defaultValue = "20") int size,
                                              @RequestParam(name = "status", required = false) Boolean status) {
        return this.studentContractService.getAllStudents(page, size, status);
    }

    @PreAuthorize("hasAnyRole('OWNER','PRINCIPAL','DEVELOPER','ADMIN','RECEPTION', 'CASHIER')")
    @GetMapping("/get-all-deleted-students")
    public ApiResponse getAllDeletedStudents(@RequestParam(name = "page", defaultValue = "0") int page,
                                             @RequestParam(name = "size", defaultValue = "20") int size) {
        return this.studentContractService.getDeletedStudents(page, size);
    }

    @PreAuthorize("hasAnyRole('OWNER','PRINCIPAL','DEVELOPER','ADMIN','RECEPTION', 'RECEPTION','EDUCATIONAL_DEPARTMENT','CASHIER')")
    @GetMapping("/download-reminder")
    public void downloadReminderFilter(
            @RequestParam(value = "studentName", required = false) String studentName,
            @RequestParam(value = "months", required = false) Months months,
            @RequestParam(value = "isReminder", required = false) Boolean isReminded,
            @RequestParam(value = "estimatedTime", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalDate estimatedTime,
            @RequestParam(value = "comment", required = false) String comment,
            @RequestParam(value = "gender", required = false) Gender gender,
            @RequestParam(value = "grade", required = false) StudentGrade studentGrade,
            @RequestParam(value = "stGrade", required = false) Grade stGrade,
            @RequestParam(value = "status", required = false) Boolean status,
            @RequestParam(value = "deleted", required = false) Boolean deleted,
            @RequestParam(value = "academicYear", required = false) String academicYear,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpServletResponse response) throws IOException {
        StudentReminderFilter filter = new StudentReminderFilter();
        filter.setStudentName(studentName);
        filter.setMonths(months);
        filter.setReminded(isReminded);
        filter.setComment(comment);
        filter.setGender(gender);
        filter.setGrade(studentGrade);
        filter.setStGrade(stGrade);
        filter.setStatus(status);
        filter.setDeleted(deleted);
        filter.setAcademicYear(academicYear);
        filter.setFrom(from);
        filter.setTo(to);
        filter.setEstimatedPaymentTime(estimatedTime);
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=ESLATMALAR.xlsx");
        this.studentContractService.reminderFilter(filter, response.getOutputStream());

    }

    @PreAuthorize("hasAnyRole('OWNER','PRINCIPAL','DEVELOPER','ADMIN','RECEPTION', 'RECEPTION','EDUCATIONAL_DEPARTMENT','CASHIER')")
    @GetMapping("/filter")
    public ApiResponse filter(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "deleted", required = false, defaultValue = "false") Boolean deleted,
            @RequestParam(value = "grade", required = false) StudentGrade grade,
            @RequestParam(value = "stGrade", required = false) Grade stGrade,
            @RequestParam(value = "gender", required = false) Gender gender,
            @RequestParam(value = "language", required = false) StudyLanguage language,
            @RequestParam(value = "guardianType", required = false) GuardianType guardianType,
            @RequestParam(value = "status", required = false) Boolean status,
            @RequestParam(value = "clientType", required = false) ClientType clientType,
            @RequestParam(value = "months", required = false) Months months,
            @RequestParam(value = "paymentStatus", required = false) PaymentStatus paymentStatus,
            @RequestParam(value = "createdAtFrom", required = false) LocalDate createdAtFrom,
            @RequestParam(value = "createdAtTo", required = false) LocalDate createdAtTo,
            @RequestParam(value = "tariffName", required = false) String tariffName,
            @RequestParam(value = "academicYear", required = false) String academicYear,
            @RequestParam(value = "tariffStatus", required = false) TariffStatus tariffStatus,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "20") int size) {

        StudentContractFilter filter = new StudentContractFilter();
        filter.setKeyword(keyword);
        filter.setDeleted(deleted);
        filter.setGender(gender);
        filter.setGrade(grade);
        filter.setStGrade(stGrade);
        filter.setLanguage(language);
        filter.setGuardianType(guardianType);
        filter.setStatus(status);
        filter.setClientType(clientType);
        filter.setMonths(months);
        filter.setPaymentStatus(paymentStatus);
        filter.setTariffName(tariffName);
        filter.setAcademicYear(academicYear);
        filter.setTariffStatus(tariffStatus);
        filter.setCreatedAtFrom(createdAtFrom);
        filter.setCreatedAtTo(createdAtTo);
        return this.studentContractService.filter(filter, PageRequest.of(page, size));
    }


    @PreAuthorize("hasAnyRole('DEVELOPER','ADMIN','RECEPTION','CASHIER')")
    @GetMapping("/download-all-students-sheet")
    public void downloadExcel(
            @RequestParam(value = "gender", required = false) Gender gender,
            @RequestParam(value = "language", required = false) StudyLanguage language,
            @RequestParam(value = "guardianType", required = false) GuardianType guardianType,
            @RequestParam(value = "status", required = false) Boolean status,
            @RequestParam(value = "clientType", required = false) ClientType clientType,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "deleted", required = false, defaultValue = "false") Boolean deleted,
            @RequestParam(value = "paymentStatus", required = false) PaymentStatus paymentStatus,
            @RequestParam(value = "academicYear", required = false) String academicYear,
            @RequestParam(value = "grade", required = false) StudentGrade grade,
            @RequestParam(value = "stGrade", required = false) Grade stGrade,
            @RequestParam(value = "month", required = false) Months month,
            @RequestParam(value = "isTransactions", required = false, defaultValue = "true") Boolean isTransactions,
            HttpServletResponse response
    ) throws IOException {
        StudentContractFilter filter = new StudentContractFilter();
        filter.setGender(gender);
        filter.setLanguage(language);
        filter.setGuardianType(guardianType);
        filter.setStatus(status);
        filter.setClientType(clientType);
        filter.setCreatedAtFrom(from);
        filter.setCreatedAtTo(to);
        filter.setDeleted(deleted);
        filter.setAcademicYear(academicYear);
        filter.setPaymentStatus(paymentStatus);
        filter.setMonths(month);
        filter.setGrade(grade);
        filter.setStGrade(stGrade);
        filter.setIsTransactions(isTransactions);
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=students.xlsx");
        this.studentContractService.downloadExcel(filter, response.getOutputStream());

    }

    @PreAuthorize("hasAnyRole('DEVELOPER', 'RECEPTION','CASHIER','ADMIN')")
    @PostMapping(value = "/upload-students-sheet", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void importExcelFile(@RequestParam("file") MultipartFile file) throws IOException {
        studentContractService.uploadExcel(file);
        log.info("File imported successfully...: {}", file.getOriginalFilename());
    }

    @DeleteMapping("/delete-student-list")
    public ApiResponse deleteStudents(@RequestParam("ids") List<Long> ids) {
        return this.studentContractService.deleteStudents(ids);
    }

    @PreAuthorize("hasAnyRole('OWNER','PRINCIPAL','DEVELOPER','ADMIN','RECEPTION')")
    @GetMapping("/period-statistics")
    public ApiResponse getDailyStats(
            @RequestParam int month,
            @RequestParam int year) {
        return this.studentContractService.getMonthlyContractStats(month, year);
    }

    @PreAuthorize("hasAnyRole('OWNER','PRINCIPAL','DEVELOPER','ADMIN','RECEPTION','MARKETING')")
    @GetMapping("/get-contract-statistics")
    public ApiResponse getContractStatistics(@RequestParam("periodType") PeriodType periodType,
                                             @RequestParam(value = "status", required = false) Boolean status,
                                             @RequestParam(value = "startDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
                                             @RequestParam(value = "endDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
                                             @RequestParam(value = "year", required = false) Integer year) {
        return this.studentContractService.getContractStatistics(periodType, status, startDate, endDate, year);
    }

    @PreAuthorize("hasAnyRole('OWNER','PRINCIPAL','DEVELOPER','ADMIN','RECEPTION','MARKETING')")
    @GetMapping("/get-students-by-gender")
    public ApiResponse getStudentsByGender(@RequestParam(value = "academicYear") String academicYear,
                                           @RequestParam(value = "status", defaultValue = "true") Boolean status) {
        return this.studentContractService.getData(academicYear, status);
    }

    @PreAuthorize("hasAnyRole('OWNER','PRINCIPAL','DEVELOPER','ADMIN','RECEPTION','MARKETING')")
    @GetMapping("/get-students-by-grade")
    public ApiResponse getStudentsByGrade(@RequestParam(value = "academicYear") String academicYear,
                                          @RequestParam(value = "status", defaultValue = "true") Boolean status,
                                          @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                          @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to

    ) {
        return this.studentContractService.getInfo(academicYear, status, from, to);
    }

    @PreAuthorize("hasAnyRole('OWNER','PRINCIPAL','DEVELOPER','ADMIN','RECEPTION')")
    @GetMapping("/get-students-by-tariff")
    public ApiResponse getStudentsByTariff(@RequestParam("academicYear") String academicYear) {
        return this.studentContractService.getDataWithTariff(academicYear);
    }

    @Hidden
    @PostMapping("/pay-for-student/{studentContractId}")
    public ApiResponse payForStudent(@PathVariable(value = "studentContractId") Long studentContractId,
                                     @RequestBody StudentContractDto.StudentPaymentDto dto) {
        return this.studentContractService.payForStudent(studentContractId, dto);
    }

    @PreAuthorize("hasAnyRole('DEVELOPER','ADMIN','CASHIER')")
    @PostMapping("/payTuition/{contractId}")
    public HttpEntity<ApiResponse> payStudentTuition(@PathVariable(value = "contractId") Long studentContractId,
                                         @RequestBody StudentContractDto.StudentPaymentDto dto) {
        ApiResponse apiResponse= this.studentContractService.payStudentTuition(studentContractId, dto);
        return ResponseEntity.status(apiResponse.getStatus()).body(apiResponse);
    }

    @PreAuthorize("hasAnyRole('DEVELOPER','ADMIN','CASHIER')")
    @PutMapping("/update-tuition/{transactionId}")
    public ApiResponse updateStudentTuition(@PathVariable(value = "transactionId") Long transactionId,
                                            @RequestBody StudentContractDto.StudentPaymentDto dto) {
        return this.studentContractService.updateStudentTuition(transactionId, dto);
    }


    @PreAuthorize("hasAnyRole('DEVELOPER', 'RECEPTION','CASHIER')")
    @GetMapping("/by-contract/{contractId}")
    public ApiResponse getPaymentInfo(@PathVariable Long contractId,
                                      @RequestParam("academicYear") String academicYear) {
        return this.studentContractService.getPaymentInfoByContractId(contractId, academicYear);
    }

    @PreAuthorize("hasAnyRole('DEVELOPER', 'RECEPTION','CASHIER')")
    @PutMapping("/terminate-student-contract/{id}")
    public ApiResponse terminateContract(@PathVariable(value = "id") Long id) {
        return this.studentContractService.terminateContract(id);
    }

    @Hidden
    @GetMapping("/student/{contractId}/monthly-payments")
    public ResponseEntity<?> getMonthlyPayments(@PathVariable Long contractId) {
        List<MonthlyPaymentInfoDto> data = this.studentContractService.getAllMonthsPaymentInfo(contractId);
        return ResponseEntity.ok(Map.of(
                "message", "SUCCESS",
                "status", "OK",
                "data", data
        ));
    }

    @PreAuthorize("hasAnyRole('DEVELOPER', 'RECEPTION','CASHIER')")
    @PostMapping("/add-reminder/{studentContractId}")
    public ApiResponse addReminder(@PathVariable Long studentContractId,
                                   @RequestBody ReminderDto.ReminderCreateAndUpdateDto reminderDto) {
        return this.studentContractService.addReminder(studentContractId, reminderDto);
    }

    @PreAuthorize("hasAnyRole('DEVELOPER', 'RECEPTION','CASHIER')")
    @GetMapping("/get-student-reminders/{studentContractId}")
    public ApiResponse getStudentReminders(@PathVariable Long studentContractId) {
        return this.studentContractService.getStudentReminders(studentContractId);
    }

    @PreAuthorize("hasAnyRole('DEVELOPER', 'RECEPTION','CASHIER')")
    @PutMapping("/transfer-students")
    public ApiResponse transferStudents(
            @RequestParam(value = "studentIds") List<Long> studentIds,
            @RequestParam(value = "studentGrade", required = false) StudentGrade studentGrade,
            @RequestParam(value = "grade", required = false) Grade grade,
            @RequestParam(value = "academicYear", required = false) String academicYear,
            @RequestParam(value = "tariffId", required = false) Long tariffId,
            @RequestParam(value = "contractStartDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate contractStartDate,
            @RequestParam(value = "contractEndDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate contractEndDate
    ) {
        return this.studentContractService.transferStudents(studentIds, studentGrade, grade, academicYear, tariffId, contractStartDate, contractEndDate);
    }

    @PreAuthorize("hasAnyRole('OWNER','PRINCIPAL','DEVELOPER','ADMIN','CASHIER')")
    @GetMapping("/transaction-filter")
    public ApiResponse filter(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "deleted", required = false, defaultValue = "false") Boolean deleted,
            @RequestParam(value = "months", required = false) Months months,
            @RequestParam(value = "paymentType", required = false) PaymentType paymentType,
            @RequestParam(value = "type", required = false) TransactionType type,
            @RequestParam(value = "academicYear", required = false) String academicYear,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "20") int size) {

        TransactionFilter filter = new TransactionFilter();
        filter.setDeleted(deleted);
        filter.setMonth(months);
        filter.setPaymentType(paymentType);
        filter.setKeyword(keyword);
        filter.setType(type);
        filter.setAcademicYear(academicYear);
        filter.setFrom(from);
        filter.setTo(to);

        return this.studentContractService.transactionFilter(filter, PageRequest.of(page, size));
    }

    @PreAuthorize("hasAnyRole('OWNER','PRINCIPAL','DEVELOPER','ADMIN','CASHIER')")
    @GetMapping("/expense-transaction-filter")
    public ApiResponse expenseFilter(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "deleted", required = false, defaultValue = "false") Boolean deleted,
            @RequestParam(value = "paymentType", required = false) PaymentType paymentType,
            @RequestParam(value = "type", required = false) TransactionType type,
            @RequestParam(value = "categoryName", required = false) String categoryName,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "20") int size) {

        ExpenseTransactionFilter filter = new ExpenseTransactionFilter();
        filter.setDeleted(deleted);
        filter.setPaymentType(paymentType);
        filter.setKeyword(keyword);
        filter.setType(type);
        filter.setCategoryName(categoryName);
        filter.setFrom(from);
        filter.setTo(to);

        return this.studentContractService.expenseTransactionFilter(filter, PageRequest.of(page, size));
    }

    @PreAuthorize("hasAnyRole('DEVELOPER','ADMIN','CASHIER','RECEPTION')")
    @PutMapping("/update-student-tariff/{studentContractId}")
    public ApiResponse updateStudentTariff(@PathVariable(value = "studentContractId") Long studentContractId,
                                           @RequestParam(value = "tariffId") Long tariffId,
                                           @RequestParam(value = "reason") String reason) {
        return this.studentContractService.updateStudentTariff(studentContractId, tariffId, reason);
    }

    @PreAuthorize("hasAnyRole('DEVELOPER','ADMIN','CASHIER','RECEPTION')")
    @GetMapping("/get-all-tariff-history")
    public ApiResponse getTariffHistory(@RequestParam(value = "studentContractId") Long studentContractId) {
        return this.studentContractService.getAllTariffHistory(studentContractId);
    }


}