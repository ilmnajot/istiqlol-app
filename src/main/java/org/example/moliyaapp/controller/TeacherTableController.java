package org.example.moliyaapp.controller;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletResponse;
import org.example.moliyaapp.dto.AdvancedPaymentDto;
import org.example.moliyaapp.dto.ApiResponse;
import org.example.moliyaapp.dto.StudentContractDto;
import org.example.moliyaapp.dto.TeacherTableDto;
import org.example.moliyaapp.enums.Months;
import org.example.moliyaapp.enums.Role;
import org.example.moliyaapp.filter.TeacherTabelSheetFilter;
import org.example.moliyaapp.filter.TeacherTableFilter;
import org.example.moliyaapp.service.TeacherTableService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/teacherTable")
public class TeacherTableController {

    private final TeacherTableService teacherTableService;

    public TeacherTableController(TeacherTableService teacherTableService) {
        this.teacherTableService = teacherTableService;
    }

    @PreAuthorize("hasAnyRole('DEVELOPER','HR', 'EDUCATIONAL_DEPARTMENT')")
    @PostMapping("/add-teacher-table")
    public HttpEntity<ApiResponse> addTeacherTable(@RequestBody TeacherTableDto.CreateTeacherTableDto dto) {
        ApiResponse apiResponse = this.teacherTableService.addTeacherTable(dto);
        return ResponseEntity.status(apiResponse.getStatus()).body(apiResponse);
    }

    @PreAuthorize("hasAnyRole('DEVELOPER', 'OWNER', 'PRINCIPAL', 'ADMIN', 'HR', 'EDUCATIONAL_DEPARTMENT')")
    @GetMapping("/get-teacher-table/{id}")
    public ApiResponse getTeacherTable(@PathVariable Long id) {
        return this.teacherTableService.getTeacherTableById(id);
    }

    @PreAuthorize("hasAnyRole('DEVELOPER', 'OWNER', 'PRINCIPAL', 'ADMIN', 'HR', 'EDUCATIONAL_DEPARTMENT')")
    @GetMapping("/get-all-teacher-table-by-month-and-year")
    public ApiResponse getTeacherTableByIdAndMonth(@RequestParam(value = "month") Months month,
                                                   @RequestParam(value = "year") Integer year) {
        return this.teacherTableService.getAllTeacherTableByMonth(month, year);
    }

    @PreAuthorize("hasAnyRole('DEVELOPER', 'OWNER', 'PRINCIPAL', 'ADMIN', 'HR', 'EDUCATIONAL_DEPARTMENT')")
    @GetMapping("/get-all-teacher-tables-list")
    public ApiResponse getAllTeacherTableList() {
        return this.teacherTableService.getAllTeacherTableList();
    }

    @PreAuthorize("hasAnyRole('HR','EDUCATIONAL_DEPARTMENT','DEVELOPER','ADMIN','OWNER')")
    @GetMapping("/get-all-teacher-table-page")
    public ApiResponse getAllTeacherTablePage(@RequestParam(value = "page", defaultValue = "0") int page,
                                              @RequestParam(value = "size", defaultValue = "10") int size) {
        return this.teacherTableService.getAllTeacherTablePage(PageRequest.of(page, size));
    }

    @Hidden
    @DeleteMapping("/delete-teacher-table/{id}")
    public ApiResponse deleteTeacherTable(@PathVariable Long id) {
        return this.teacherTableService.deleteTeacherTable(id);
    }

    @PreAuthorize("hasAnyRole('HR','EDUCATIONAL_DEPARTMENT','DEVELOPER','ADMIN')")
    @PutMapping("/update-teacher-table/{id}")
    public ApiResponse updateTeacherTable(@PathVariable Long id,
                                          @RequestBody TeacherTableDto.CreateTeacherTableDto dto) {
        return this.teacherTableService.updateTeacherTable(id, dto);
    }

    @PreAuthorize("hasAnyRole('HR','EDUCATIONAL_DEPARTMENT','DEVELOPER','ADMIN')")
    @GetMapping("/get-by-teacher-table-by-teacherId-and-month/{teacherId}")
    public ApiResponse getTeacherTableByID(@PathVariable(value = "teacherId") Long teacherId,
                                           @RequestParam(value = "month") Months month) {
        return this.teacherTableService.getByTeacherIdAndMonth(teacherId, month);
    }

    @Hidden
    @GetMapping("/get-all-users-tables-by-user-role/hidden")
    public ApiResponse getAllUsersByRole(@RequestParam(value = "role", required = false) Role role,
                                         @RequestParam(value = "page", defaultValue = "0") Integer page,
                                         @RequestParam(value = "size", defaultValue = "20") Integer size,
                                         @RequestParam(value = "other_roles", required = false) Boolean otherRoles) {
        return this.teacherTableService.getAllUsersByRole(role, PageRequest.of(page, size), otherRoles);
    }

    @PreAuthorize("hasAnyRole('HR','EDUCATIONAL_DEPARTMENT','DEVELOPER','ADMIN','OWNER','PRINCIPAL')")
    @GetMapping("/get-all-users-tables-by-user-role")
    public ApiResponse filter(
            @RequestParam(value = "deleted", required = false) Boolean deleted,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "month", required = false) Months month,
            @RequestParam(value = "role", required = false) Role role,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "otherRoles", required = false) Boolean otherRoles,
            @RequestParam(value = "createdAtFrom", required = false) LocalDate createdAtFrom,
            @RequestParam(value = "createdAtTo", required = false) LocalDate createdAtTo,
            @RequestParam(value = "page", defaultValue = "0", required = false) Integer page,
            @RequestParam(value = "size", defaultValue = "20", required = false) Integer size) {
        TeacherTableFilter filter = new TeacherTableFilter();
        filter.setDeleted(deleted);
        filter.setKeyword(keyword);
        filter.setMonths(month);
        filter.setRole(role);
        filter.setOtherRoles(otherRoles);
        filter.setYear(year);
        filter.setCreatedAtFrom(createdAtFrom);
        filter.setCreatedAtTo(createdAtTo);
        return this.teacherTableService.filter(PageRequest.of(page, size), filter);
    }

    @PreAuthorize("hasAnyRole('HR','EDUCATIONAL_DEPARTMENT','DEVELOPER','ADMIN','PRINCIPAL')")
    @GetMapping("/get-all-teacher-contracts-by-month")
    public ApiResponse getAllTCByMonths(@RequestParam(value = "month", required = false) Months months,
                                        @RequestParam(value = "page", defaultValue = "0") int page,
                                        @RequestParam(value = "size", defaultValue = "20") int size) {
        return this.teacherTableService.getAllByMonth(months, PageRequest.of(page, size));
    }

    @PreAuthorize("hasAnyRole('HR','EDUCATIONAL_DEPARTMENT','DEVELOPER','ADMIN')")
    @GetMapping("/search-teacher-table")
    public ApiResponse search(@RequestParam(value = "keyword") String keyword,
                              @RequestParam(value = "page", defaultValue = "0") int page,
                              @RequestParam(value = "size", defaultValue = "20") int size) {
        return this.teacherTableService.searchByName(keyword, PageRequest.of(page, size));
    }

    @Hidden
    @DeleteMapping("/delete-tabel-list")
    public HttpEntity<ApiResponse> deleteTabels(@RequestParam("ids") List<Long> ids) {
        ApiResponse apiResponse = this.teacherTableService.deleteConctracts(ids);
        return ResponseEntity.status(apiResponse.getStatus()).body(apiResponse);

    }


    @PreAuthorize("hasAnyRole('DEVELOPER','ADMIN')")
    @PostMapping("/pay-cash/{tableId}")
    public HttpEntity<ApiResponse> payForCash(@PathVariable(value = "tableId") Long tableId,
                                              @RequestParam(value = "amount") Double amount,
                                              @RequestParam(value = "bonus", required = false) Double bonus,
                                              @RequestParam(value = "comment", required = false) String comment) {
        ApiResponse apiResponse = this.teacherTableService.payForCash(tableId, amount, bonus, comment);
        return ResponseEntity.status(apiResponse.getStatus()).body(apiResponse);
    }


    @PreAuthorize("hasAnyRole('DEVELOPER','ADMIN')")
    @PostMapping("/pay-advance-cash/{employeeContractId}")
    public HttpEntity<ApiResponse> payAdvanceForCash(@PathVariable(value = "employeeContractId") Long employeeContractId,
                                                     @RequestBody AdvancedPaymentDto dto) {
        String comment = dto.getComment();
        Months month = dto.getMonth();
        Boolean isAdvanced = dto.getIsAdvanced();
        Double amount = dto.getAmount();
        ApiResponse apiResponse = this.teacherTableService.payAdvanceForCash(employeeContractId, amount, month, comment, isAdvanced);
        return ResponseEntity.status(apiResponse.getStatus()).body(apiResponse);
    }


    @PreAuthorize("hasAnyRole('DEVELOPER','ADMIN')")
    @PostMapping("/pay-card")
    public HttpEntity<ApiResponse> payForCard(@RequestParam(value = "employeeIds") List<Long> employeeIds,
                                              @RequestParam(value = "month") Months months,
                                              @RequestParam(value = "amount") Double amount) {
        ApiResponse apiResponse = this.teacherTableService.payForCard(employeeIds, months, amount);
        return ResponseEntity.status(apiResponse.getStatus()).body(apiResponse);
    }

    @PreAuthorize("hasAnyRole('EDUCATIONAL_DEPARTMENT','DEVELOPER','ADMIN','OWNER','PRINCIPAL')")
    @GetMapping("/download-tabel-sheet")
    public void downloadExcel(
            @RequestParam(value = "month", required = false) Months month,
            @RequestParam(value = "createdAtFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdAtFrom,
            @RequestParam(value = "createdAtTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdAtTo,
            @RequestParam(value = "deleted", required = false, defaultValue = "false") Boolean deleted,
            HttpServletResponse response
    ) throws IOException {
        TeacherTabelSheetFilter filter = new TeacherTabelSheetFilter();
        filter.setMonths(month);
        if (createdAtFrom != null) {
            filter.setCreatedAtFrom(createdAtFrom.atStartOfDay()); // 00:00:00
        }
        if (createdAtTo != null) {
            filter.setCreatedAtTo(createdAtTo.atTime(LocalTime.MAX)); // 23:59:59.999999999
        }
        filter.setDeleted(deleted);
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=xodimlar-tabellar.xls");
       this.teacherTableService.downloadExcel(filter, response.getOutputStream());

    }


    @PreAuthorize("hasAnyRole('DEVELOPER','ADMIN','CASHIER')")
    @PutMapping("/update-salary/{transactionId}")
    public HttpEntity<ApiResponse> updateEmployeeTuition(@PathVariable(value = "transactionId") Long transactionId,
                                                         @RequestBody StudentContractDto.EmployeePaymentDto dto) {
        ApiResponse apiResponse = this.teacherTableService.updateEmployeeTuition(transactionId, dto);
        return ResponseEntity.status(apiResponse.getStatus()).body(apiResponse);
    }

    @PreAuthorize("hasAnyRole('DEVELOPER','ADMIN','CASHIER')")
    @PutMapping("/update-bonus/{bonusId}")
    public HttpEntity<ApiResponse> updateBonusTuition(@PathVariable(value = "bonusId") Long bonusId,
                                                      @RequestBody StudentContractDto.EmployeeBonusPaymentDto dto) {
        ApiResponse apiResponse = this.teacherTableService.updateEmployeeBonusTuition(bonusId, dto);
        return ResponseEntity.status(apiResponse.getStatus()).body(apiResponse);
    }

    @PreAuthorize("hasAnyRole('DEVELOPER','ADMIN','HR','EDUCATIONAL_DEPARTMENT')")
    @DeleteMapping("/delete-teacher-table-list")
    public HttpEntity<ApiResponse> deleteTeacherTableList(@RequestParam("ids") List<Long> ids) {
        ApiResponse apiResponse = this.teacherTableService.deleteTables(ids);
        return ResponseEntity.status(apiResponse.getStatus()).body(apiResponse);
    }
}
