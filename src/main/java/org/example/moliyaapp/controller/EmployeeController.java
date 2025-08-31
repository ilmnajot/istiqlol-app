package org.example.moliyaapp.controller;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.example.moliyaapp.dto.ApiResponse;
import org.example.moliyaapp.dto.RegisterDto;
import org.example.moliyaapp.dto.UserDto;
import org.example.moliyaapp.enums.Months;
import org.example.moliyaapp.enums.Role;
import org.example.moliyaapp.filter.EmployeeFilter;
import org.example.moliyaapp.filter.EmployeeSheetFilter;
import org.example.moliyaapp.filter.TeacherTabelSheetFilter;
import org.example.moliyaapp.service.EmployeeService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/employees")
public class EmployeeController {
    private final EmployeeService employeeService;

    @Hidden
    @PostMapping("/createStudent")
    public ApiResponse createStudent(@RequestBody UserDto.CreateStudent student) {
        return this.employeeService.createStudent(student);
    }

    @Hidden
    @PostMapping("/createTeacher")
    public ApiResponse createTeacher(UserDto.CreateEmployee teacher) {
        return this.employeeService.createTeacher(teacher);
    }

    @Hidden
    @GetMapping("/getAllUserByRoleName")
    public ApiResponse getAllUserByRole(@RequestParam("role") String role) {
        return this.employeeService.getAllUserByRole(role);
    }

    //********************EMPLOYEE*****************//
    @PreAuthorize("hasAnyRole('HR','DEVELOPER','ADMIN')")
    @Hidden
    @PostMapping("/add-employee")
    public ApiResponse addNewEmployee(@RequestBody RegisterDto.RegisterEmployee dto) {
        return this.employeeService.createEmployee(dto);
    }

    @PreAuthorize("hasAnyRole('DEVELOPER', 'OWNER', 'PRINCIPAL', 'ADMIN', 'HR','RECEPTION','EDUCATIONAL_DEPARTMENT','STUDENT_CONTRACT_MAKER','')")
    @GetMapping("/get-employee/{id}")
    public ApiResponse getEmployee(@PathVariable Long id) {
        return this.employeeService.getEmployee(id);
    }

    @PreAuthorize("hasAnyRole('DEVELOPER', 'OWNER', 'PRINCIPAL', 'ADMIN', 'HR')")
    @GetMapping("/get-all-active-employees")
    public ApiResponse getAllActiveEmployee(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "role", required = false) Role role,
            @RequestParam(value = "deleted", required = false) Boolean deleted,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        EmployeeFilter filter = new EmployeeFilter();
        filter.setKeyword(keyword);
        filter.setRole(role);
        filter.setDeleted(deleted);
        return this.employeeService.getAllActiveEmployees(PageRequest.of(page, size, Sort.Direction.DESC, "createdAt"), filter);
    }

    @Hidden
    @GetMapping("/get-all-Inactive-employees")
    public ApiResponse getAllInActiveEmployee(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "role", required = false) Role role,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        EmployeeFilter filter = new EmployeeFilter();
        filter.setKeyword(keyword);
        filter.setRole(role);
        return this.employeeService.getAllInActiveEmployees(PageRequest.of(page, size, Sort.Direction.DESC, "createdAt"), filter);
    }

    @Hidden
    @GetMapping("/get-all-employees-by-role-name")
    public ApiResponse getByPosition(@RequestParam(name = "role") Long roleId,
                                     @RequestParam(name = "page", defaultValue = "0") int page,
                                     @RequestParam(name = "size", defaultValue = "20") int size) {
        return this.employeeService.getAllByRole(roleId, page, size);
    }

    @PreAuthorize("hasAnyRole('HR','DEVELOPER')")
    @DeleteMapping("/delete-employee/{id}")
    public HttpEntity<ApiResponse> deleteEmployee(@PathVariable(name = "id") Long id) {
        ApiResponse apiResponse = this.employeeService.deleteEmployee(id);
        return ResponseEntity.status(apiResponse.getStatus()).body(apiResponse);
    }

    @PreAuthorize("hasAnyRole('HR','DEVELOPER','MARKETING','ADMIN','RECEPTION','CASHIER','EDUCATIONAL_DEPARTMENT')")
    @PutMapping("/update-employee/{id}")
    public ApiResponse updateEmployee(@PathVariable(name = "id") Long id,
                                      @RequestBody UserDto.UpdateUser dto) {
        return this.employeeService.updateEmployee(id, dto);
    }

    @Hidden
    @GetMapping("/get-teachers-by-grade")
    public ApiResponse getTeachersByGroup(@RequestParam(name = "groupId") Long groupId,
                                          @RequestParam(name = "page", defaultValue = "0") int page,
                                          @RequestParam(name = "size", defaultValue = "20") int size) {
        return this.employeeService.getAllTeachersByGroup(groupId, page, size);
    }

    @Hidden
    @GetMapping("/filter")
    public ApiResponse filter(@RequestBody EmployeeFilter filter,
                              @RequestParam(name = "page", defaultValue = "0") int page,
                              @RequestParam(name = "size", defaultValue = "20") int size) {
        return this.employeeService.filter(filter, page, size);
    }

    @PreAuthorize("hasAnyRole('HR','DEVELOPER','OWNER','PRINCIPAL','ADMIN','EDUCATIONAL_DEPARTMENT')")
    @GetMapping("/get-all-employee-list")
    public ApiResponse getAllEmployeeList() {
        return this.employeeService.getAllEmployeeList();
    }

    @Hidden
    @GetMapping("/search-by-name-and-phone-number-and-contract-number")
    public ApiResponse searchEmployee(@RequestParam(value = "keyword") String keyword,
                                      @RequestParam(value = "page", defaultValue = "0") Integer page,
                                      @RequestParam(value = "size", defaultValue = "20") Integer size) {
        return this.employeeService.searchEmployee(keyword, PageRequest.of(page, size));
    }

    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    @DeleteMapping("/delete-user-list-by-ids-in-archive")
    public ApiResponse deleteUsers(@RequestParam("ids") List<Long> ids) {
        return this.employeeService.deleteUsers(ids);
    }

    @PreAuthorize("hasAnyRole('HR','DEVELOPER','ADMIN')")
    @GetMapping("/download-employee-sheet")
    public ResponseEntity<InputStreamResource> downloadExcel(
            @RequestParam(value = "createdAtFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdAtFrom,
            @RequestParam(value = "createdAtTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdAtTo,
            @RequestParam(value = "deleted", required = false, defaultValue = "false") Boolean deleted

    ) {
        EmployeeSheetFilter filter = new EmployeeSheetFilter();
        if (createdAtFrom != null) {
            filter.setCreatedAtFrom(createdAtFrom.atStartOfDay()); // 00:00:00
        }
        if (createdAtTo != null) {
            filter.setCreatedAtTo(createdAtTo.atTime(LocalTime.MAX)); // 23:59:59.999999999
        }
        filter.setDeleted(deleted);
        ByteArrayInputStream toExcel = this.employeeService.downloadExcel(filter);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=xodimlar.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(toExcel));
    }

}
