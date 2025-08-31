package org.example.moliyaapp.controller;

import lombok.RequiredArgsConstructor;
import org.example.moliyaapp.dto.ApiResponse;
import org.example.moliyaapp.dto.SmsDto;
import org.example.moliyaapp.enums.Months;
import org.example.moliyaapp.enums.TariffStatus;
import org.example.moliyaapp.service.SmsService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/notify.eskiz.uz/api")
public class SmsController {
    private final SmsService smsService;

    //balance ni olish
    @PreAuthorize("hasAnyRole('DEVELOPER', 'CASHIER','ADMIN')")
    @GetMapping("/user/get-limit")
    public ApiResponse getLimit() {
        return this.smsService.getLimit();
    }

   //to get ready templete
    @PreAuthorize("hasAnyRole('DEVELOPER', 'CASHIER','ADMIN')")
    @GetMapping("/user/templates")
    public ApiResponse getTemplates() {
        return this.smsService.getTemplates();
    }

    //to send a message
    @PreAuthorize("hasAnyRole('DEVELOPER', 'CASHIER','ADMIN')")
    @PostMapping("/message/sms/send")
    public ResponseEntity<?> sendToSpecificStudents(
            @RequestParam(value = "ids") List<Long> studentContractIds,
            @RequestParam(value = "month", required = false) Months month
            ) {
        this.smsService.sendToSpecificStudents(studentContractIds, month);
        return ResponseEntity.ok(Map.of(
                "success",
                true,
                "message",
                "SMS muvaffaqiyatli yuborildi."));
    }

    //to get total statistics
    @PreAuthorize("hasAnyRole('DEVELOPER', 'CASHIER','ADMIN')")
    @PostMapping("/user/totals")
    public HttpEntity<ApiResponse> getSMSReport(@RequestBody SmsDto.SmsReportDto dto) {
        ApiResponse apiResponse = this.smsService.getSMSReport(dto);
        return ResponseEntity.status(apiResponse.getStatus()).body(apiResponse);
    }

    // to get history
    @PreAuthorize("hasAnyRole('DEVELOPER', 'CASHIER','ADMIN')")
    @PostMapping("/sms/history")
    public ResponseEntity<ApiResponse> getSMSHistory(@RequestBody SmsDto.SmsHistoryDto dto) {
        return ResponseEntity.ok(smsService.getSMSHistory(dto));
    }

    // download history
    @PreAuthorize("hasAnyRole('DEVELOPER', 'CASHIER','ADMIN')")
    @GetMapping("/sms/export/download")
    public ResponseEntity<Resource> downloadMessagesCSV(
            @RequestParam int year,
            @RequestParam int month) {
        try {
            ApiResponse response = smsService.exportMessages(year, month);

            if (response.getStatus() == HttpStatus.OK && response.getData() != null) {
                String csvData = response.getData().toString();

                ByteArrayResource resource = new ByteArrayResource(csvData.getBytes(StandardCharsets.UTF_8));

                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=sms_export_" + year + "_" + month + ".csv")
                        .contentType(MediaType.parseMediaType("text/csv"))
                        .contentLength(resource.contentLength())
                        .body(resource);
            } else {
                return ResponseEntity.badRequest().build();
            }

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // history by month
    @PreAuthorize("hasAnyRole('DEVELOPER', 'CASHIER','ADMIN')")
    @GetMapping("/total-by-month")
    public ResponseEntity<ApiResponse> calculateCostByBalance(@RequestParam(value = "year") int year) {
        return ResponseEntity.ok(smsService.calculateCostByBalance(year));
    }


}
