package org.example.moliyaapp.service;

import org.example.moliyaapp.dto.ApiResponse;
import org.example.moliyaapp.enums.TariffStatus;

import java.time.LocalDate;

public interface CompanyService {
    ApiResponse getBalance(Long id, LocalDate from, LocalDate toDate);

    ApiResponse getAllByMonths(String year);

    ApiResponse getAllByMonthsEmployee(LocalDate from, LocalDate to);

//    ApiResponse getAllByTariff(String year, TariffStatus tariffStatus);

    ApiResponse getAllAmountByTariff(String year, TariffStatus tariffStatus);

//    ApiResponse getCurrentBalance(Long id, LocalDate fromDate, LocalDate toDate);
}
