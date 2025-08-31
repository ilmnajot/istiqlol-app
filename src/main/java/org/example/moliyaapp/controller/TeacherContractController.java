package org.example.moliyaapp.controller;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletResponse;
import org.example.moliyaapp.dto.ApiResponse;
import org.example.moliyaapp.dto.TeacherContractDto;
import org.example.moliyaapp.enums.Role;
import org.example.moliyaapp.enums.SalaryType;
import org.example.moliyaapp.filter.TeacherContractFilter;
import org.example.moliyaapp.filter.TeacherContractSheetFilter;
import org.example.moliyaapp.service.TeacherContractService;
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
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RequestMapping("/teacherContracts")
@RestController
public class TeacherContractController {
    private final TeacherContractService teacherContractService;

    public TeacherContractController(TeacherContractService teacherContractService) {
        this.teacherContractService = teacherContractService;
    }

    @PreAuthorize("hasAnyRole('HR','DEVELOPER')")
    @PostMapping("/add-teacher-contract")
    public HttpEntity<ApiResponse> addTeacherContract(@RequestBody TeacherContractDto.CreateContractDto dto) {
        ApiResponse apiResponse = this.teacherContractService.addTeacherContract(dto);
        return ResponseEntity.status(apiResponse.getStatus()).body(apiResponse);
    }

    @PreAuthorize("hasAnyRole('DEVELOPER', 'OWNER', 'PRINCIPAL', 'ADMIN', 'HR')")
    @GetMapping("/get-all-teacher-contracts-list")
    public ApiResponse getAllTeacherContractsList() {
        return this.teacherContractService.getAllTeacherContractsList();
    }

    @PreAuthorize("hasAnyRole('DEVELOPER', 'OWNER', 'PRINCIPAL', 'ADMIN', 'HR')")
    @GetMapping("/get-all-teacher-contracts-page")
    public ApiResponse getAllTeacherContractsPage(@RequestParam(value = "page", defaultValue = "0") Integer page,
                                                  @RequestParam(value = "size", defaultValue = "10") Integer size) {
        return this.teacherContractService.getAllTeacherContractsPage(PageRequest.of(page, size, Sort.Direction.DESC, "createdAt"));
    }

    @PreAuthorize("hasAnyRole('DEVELOPER', 'OWNER', 'PRINCIPAL', 'ADMIN', 'HR')")
    @GetMapping("/get-teacher-contract-by-id/{id}")
    public ApiResponse getTeacherContractById(@PathVariable(value = "id") Long id) {
        return this.teacherContractService.getTeacherContract(id);
    }

    @PreAuthorize("hasAnyRole('DEVELOPER', 'OWNER', 'PRINCIPAL', 'ADMIN', 'HR')")
    @GetMapping("/get-all-teacher-contracts-by-teacher-id/{teacherId}")
    public ApiResponse getAllContractsByTeacherId(@PathVariable(name = "teacherId") Long teacherId) {
        return this.teacherContractService.getContractsByTeacherId(teacherId);
    }

    @PreAuthorize("hasAnyRole('DEVELOPER','HR')")
    @DeleteMapping("/delete-teacher-contract/{id}")
    public HttpEntity<ApiResponse> deleteTeacherContract(@PathVariable(value = "id") Long id) {
        ApiResponse apiResponse = this.teacherContractService.deleteTeacherContract(id);
        return ResponseEntity.status(apiResponse.getStatus()).body(apiResponse);
    }
    @PreAuthorize("hasAnyRole('DEVELOPER','HR')")
    @PutMapping("/update-teacher-contract/{id}")
    public ApiResponse updateContract(@PathVariable Long id,
                                      @RequestBody TeacherContractDto.CreateContractDto dto) {
        return this.teacherContractService.updateContract(dto, id);
    }
    @PreAuthorize("hasAnyRole('DEVELOPER','HR','OWNER','PRINCIPAL','ADMIN')")
    @GetMapping("/get-active-contract-of-teacher/{teacherId}")
    public ApiResponse getActiveContractOfTeacher(@PathVariable Long teacherId) {
        return this.teacherContractService.getActiveContractOfTeacher(teacherId);
    }

    @Hidden
    @GetMapping("/search-teacher-contract")
    public ApiResponse search(@RequestParam(value = "keyword") String keyword,
                              @RequestParam(value = "page", defaultValue = "0") int page,
                              @RequestParam(value = "size", defaultValue = "20") int size) {
        return this.teacherContractService.searchByName(keyword, PageRequest.of(page, size));
    }
    @PreAuthorize("hasAnyRole('DEVELOPER','HR','OWNER','PRINCIPAL','ADMIN')")
    @GetMapping("/filter-and-search")
    public ApiResponse filter(
            @RequestParam(value = "active", required = false) Boolean active,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "type", required = false) SalaryType type,
            @RequestParam(value = "deleted", required = false) Boolean deleted,
            @RequestParam(value = "lessonCountsFrom", required = false) Integer lessonCountsFrom,
            @RequestParam(value = "lessonCountsTo", required = false) Integer lessonCountsTo,
            @RequestParam(value = "role", required = false) Role role,
            @RequestParam(value = "createdAtFrom", required = false) LocalDate createdAtFrom,
            @RequestParam(value = "createdAtTo", required = false) LocalDate createdTo,
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size) {
        TeacherContractFilter filter = new TeacherContractFilter();
        filter.setActive(active);
        filter.setKeyword(keyword);
        filter.setSalaryType(type);
        filter.setLessonsCountsFrom(lessonCountsFrom);
        filter.setLessonsCountsTo(lessonCountsTo);
        filter.setRole(role);
        filter.setCreatedAtFrom(createdAtFrom);
        filter.setCreatedAtTo(createdTo);
        filter.setDeleted(deleted);
        return this.teacherContractService.filter(filter, PageRequest.of(page, size));
    }
    @PreAuthorize("hasAnyRole('DEVELOPER','HR','OWNER','PRINCIPAL','ADMIN')")
    @DeleteMapping("/delete-contract-list")
    public HttpEntity<ApiResponse> deleteContracts(@RequestParam("ids") List<Long> ids) {
        ApiResponse apiResponse = this.teacherContractService.deleteContracts(ids);
        return ResponseEntity.status(apiResponse.getStatus()).body(apiResponse);
    }

    @PreAuthorize("hasAnyRole('DEVELOPER','HR')")
    @PutMapping("/terminate-contract/{id}")
    public HttpEntity<ApiResponse> terminateContract(@PathVariable("id") Long contractId,
                                                     @RequestParam(value = "status", required = false) Boolean status,
                                                     @RequestParam(value = "date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        ApiResponse apiResponse = this.teacherContractService.terminateAndActivateContract(contractId, date, status);
        return ResponseEntity.status(apiResponse.getStatus()).body(apiResponse);
    }
    @PreAuthorize("hasAnyRole('DEVELOPER','HR','OWNER','PRINCIPAL','ADMIN')")
    @GetMapping("/download-teacher-contract-sheet")
    public void downloadExcel(
            @RequestParam(value = "createdAtFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdAtFrom,
            @RequestParam(value = "createdAtTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdAtTo,
            @RequestParam(value = "deleted", required = false, defaultValue = "false") Boolean deleted,
            HttpServletResponse response

    ) throws IOException {
        TeacherContractSheetFilter filter = new TeacherContractSheetFilter();
        if (createdAtFrom != null) {
            filter.setCreatedAtFrom(createdAtFrom.atStartOfDay()); // 00:00:00
        }
        if (createdAtTo != null) {
            filter.setCreatedAtTo(createdAtTo.atTime(LocalTime.MAX)); // 23:59:59.999999999
        }
        filter.setDeleted(deleted);
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=xodim-shortnomalar.xls");
       this.teacherContractService.downloadExcel(filter, response.getOutputStream());

    }



}
