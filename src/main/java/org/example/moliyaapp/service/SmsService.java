package org.example.moliyaapp.service;

import org.example.moliyaapp.dto.ApiResponse;
import org.example.moliyaapp.dto.SmsDto;
import org.example.moliyaapp.enums.Months;

import java.util.List;

public interface SmsService {

    void sendToSpecificStudents(List<Long> studentContractIds, Months month);

    String getTokenByEmailAndPassword();

    ApiResponse getSMSReport(SmsDto.SmsReportDto dto);

    ApiResponse getSMSHistory(SmsDto.SmsHistoryDto dto);

    ApiResponse exportMessages(int year, int month);

    ApiResponse calculateCostByBalance(int year);

    ApiResponse getTemplates();

    ApiResponse getLimit();
}
