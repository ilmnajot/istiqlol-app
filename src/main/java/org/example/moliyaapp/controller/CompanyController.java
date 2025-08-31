package org.example.moliyaapp.controller;

import lombok.RequiredArgsConstructor;
import org.example.moliyaapp.dto.ApiResponse;
import org.example.moliyaapp.enums.TariffStatus;
import org.example.moliyaapp.service.CompanyService;
import org.example.moliyaapp.service.ExpensesService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/companies")
public class CompanyController {
    private final CompanyService companyService;

    @PreAuthorize("hasAnyRole('OWNER','PRINCIPAL','ADMIN','DEVELOPER')")
    @GetMapping("/get-balance/{id}")
    public ApiResponse getBalance(
            @PathVariable Long id,
            @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return this.companyService.getBalance(id, fromDate, toDate);
    }

    @PreAuthorize("hasAnyRole('OWNER','PRINCIPAL','ADMIN','DEVELOPER')")
    @GetMapping("/get-amount-by-months")
    public ApiResponse getAmountByMonths(@RequestParam(value = "academicYear") String year) {
        return this.companyService.getAllByMonths(year);
    }


    @PreAuthorize("hasAnyRole('OWNER','PRINCIPAL','ADMIN','DEVELOPER')")
    @GetMapping("/get-amount-by-tariff")
    public ApiResponse getAllAmountByTariff(
            @RequestParam(value = "academicYear") String year,
            @RequestParam(value = "tariffStatus") TariffStatus tariffStatus) {
        return this.companyService.getAllAmountByTariff(year, tariffStatus);
    }

    @PreAuthorize("hasAnyRole('OWNER','PRINCIPAL','ADMIN','DEVELOPER')")
    @GetMapping("/get-amount-by-months-employee")
    public ApiResponse getAmountByMonthsEmployee(@RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                 @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return this.companyService.getAllByMonthsEmployee(from, to);
    }
}
