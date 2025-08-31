package org.example.moliyaapp.service.impl;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.moliyaapp.entity.MonthlyFee;
import org.example.moliyaapp.entity.User;
import org.example.moliyaapp.repository.MonthlyFeeRepository;
import org.example.moliyaapp.repository.StudentContractRepository;
import org.example.moliyaapp.repository.TeacherTableRepository;
import org.example.moliyaapp.repository.UserRepository;
import org.example.moliyaapp.service.EmployeeMonthlyFeeGoogleSheet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class EmployeeMonthlyFeeGoogleSheetImpl implements EmployeeMonthlyFeeGoogleSheet {

    private final Sheets sheetService;
    private final StudentContractRepository studentContractRepository;
    private final UserRepository userRepository;
    private final TeacherTableRepository teacherTableRepository;
    private final MonthlyFeeRepository monthlyFeeRepository;

//    @Value("${google.sheets.student.contracts.sheet.id}")
//    private String studentTransactionsSheetId;
   @Value("${google.sheets.employee.contracts.sheet.id}")
    private String studentTransactionsSheetId;

    @Value("${google.sheets.employee-fee.range}")
    private String monthlyFeeRange;


    @Override
    public void recordEmployeeMonthlyFee(MonthlyFee monthlyFee) {

        Long createdBy = monthlyFee.getCreatedBy();
        Long updatedBy = monthlyFee.getUpdatedBy();
        User createUser = this.userRepository.findByIdAndDeletedFalse(createdBy)
                .orElse(null);
        User updateUser = this.userRepository.findByIdAndDeletedFalse(updatedBy)
                .orElse(null);

        try {
            List<Object> row = List.of(
                    monthlyFee.getId(),
                    monthlyFee.getEmployee() != null ? monthlyFee.getEmployee().getId() : 0L,
                    monthlyFee.getEmployee() != null ? monthlyFee.getEmployee().getFullName() : "",
                    monthlyFee.getMonths() != null ? monthlyFee.getMonths().name() : "",
                    monthlyFee.getTotalFee() != null ? monthlyFee.getTotalFee() : 0.0,
                    monthlyFee.getAmountPaid() != null ? monthlyFee.getAmountPaid() : 0.0,
                    monthlyFee.getRemainingBalance() != null ? monthlyFee.getRemainingBalance() : 0.0,
                    monthlyFee.getBonus() != null ? monthlyFee.getBonus() : 0.0,
                    monthlyFee.getPenalty() != null ? monthlyFee.getPenalty() : 0.0,
                    monthlyFee.getIsAdvanced() != null && monthlyFee.getIsAdvanced() ? "Ha" : "Yoq",
                    monthlyFee.getStatus() != null ?
                            switch (monthlyFee.getStatus()) {
                                case UNPAID -> "To'lanmagan".toUpperCase();
                                case FULLY_PAID -> "To'liq to'langan".toUpperCase();
                                case PARTIALLY_PAID -> "Qisman to'langan".toUpperCase();
                            } : "",
                    monthlyFee.getCreatedAt() != null ? monthlyFee.getCreatedAt().toString() : "",
                    monthlyFee.getUpdatedAt() != null ? monthlyFee.getUpdatedAt().toString() : "",
                    createUser != null ? createUser.getFullName() : "",
                    updateUser != null ? updateUser.getFullName() : "",
                    monthlyFee.getDeleted() != null && monthlyFee.getDeleted() ? "Ha" : "Yoq"
//
            );
            ValueRange valueRange = new ValueRange()
                    .setValues(Collections.singletonList(row));
            this.sheetService
                    .spreadsheets()
                    .values()
                    .append(studentTransactionsSheetId, monthlyFeeRange, valueRange)
                    .setValueInputOption("USER_ENTERED")
                    .setInsertDataOption("INSERT_ROWS")
                    .execute();

        } catch (IOException e) {
            log.info("Error happened while recording student transaction to Google Sheet: {}", e.getMessage());

        }

    }

    //to tnit mfee
    @Override
    public void initializeSheet() {
        try {
            ValueRange headerRange = this.sheetService
                    .spreadsheets()
                    .values()
                    .get(studentTransactionsSheetId, monthlyFeeRange)
                    .execute();
            if (headerRange == null || headerRange.getValues() == null || headerRange.getValues().isEmpty()) {
                List<Object> header = List.of(
                        "Oylik To'lov ID",
                        "Xodim ID",
                        "Xodim F.I.SH",
                        "Oy",
                        "Umumiy oylik to'lov",
                        "To'langan miqdor",
                        "Qoldiq",
                        "Bonus",
                        "Shtraf",
                        "Avans olganmi!?",
                        "To'lov Status",
                        "Yaratilgan vaqt",
                        "O'zgartirilgan vaqt",
                        "Kim qo'shdi!?",
                        "Kim tahrirladi!?",
                        "O'chirilganmi!?"
                );
                ValueRange valueRange = new ValueRange()
                        .setValues(Collections.singletonList(header));
                this.sheetService
                        .spreadsheets()
                        .values()
                        .append(studentTransactionsSheetId, monthlyFeeRange, valueRange)
                        .setValueInputOption("USER_ENTERED")
                        .setInsertDataOption("INSERT_ROWS")
                        .execute();
                this.bulkyAllMonthlyFee();
            }
        } catch (IOException e) {
            log.error("Error happened while initializing Google Sheet: {}", e.getMessage());
        }

    }

    private void bulkyAllMonthlyFee() throws IOException {
        List<MonthlyFee> list = this.monthlyFeeRepository.findAllEmployeeSheet();
        if (list.isEmpty()) {
            log.info("Oylik to'lovlar topilmadi!");
            return;
        }
        List<List<Object>> rows = new ArrayList<>();
        for (MonthlyFee transaction : list) {
            rows.add(toRow(transaction));
        }
        ValueRange body = new ValueRange().setValues(rows);
        sheetService.spreadsheets().values()
                .append(studentTransactionsSheetId, monthlyFeeRange, body)
                .setValueInputOption("USER_ENTERED")
                .setInsertDataOption("INSERT_ROWS")
                .execute();
        log.info("Bulk export of student transactions completed. Total records exported: {}", list.size());
    }

    private List<Object> toRow(MonthlyFee monthlyFee) {
        Long createdBy = monthlyFee.getCreatedBy();
        Long updatedBy = monthlyFee.getUpdatedBy();
        User createUser = this.userRepository.findByIdAndDeletedFalse(createdBy)
                .orElse(null);
        User updateUser = this.userRepository.findByIdAndDeletedFalse(updatedBy)
                .orElse(null);
        return List.of(
                monthlyFee.getId(),
                monthlyFee.getEmployee() != null ? monthlyFee.getEmployee().getId() : 0L,
                monthlyFee.getEmployee() != null ? monthlyFee.getEmployee().getFullName() : "",
                monthlyFee.getMonths() != null ? monthlyFee.getMonths().name() : "",
                monthlyFee.getTotalFee() != null ? monthlyFee.getTotalFee() : 0.0,
                monthlyFee.getAmountPaid() != null ? monthlyFee.getAmountPaid() : 0.0,
                monthlyFee.getRemainingBalance() != null ? monthlyFee.getRemainingBalance() : 0.0,
                monthlyFee.getBonus() != null ? monthlyFee.getBonus() : 0.0,
                monthlyFee.getPenalty() != null ? monthlyFee.getPenalty() : 0.0,
                monthlyFee.getIsAdvanced() != null && monthlyFee.getIsAdvanced() ? "Ha" : "Yoq",
                monthlyFee.getStatus() != null ?
                        switch (monthlyFee.getStatus()) {
                            case UNPAID -> "To'lanmagan".toUpperCase();
                            case FULLY_PAID -> "To'liq to'langan".toUpperCase();
                            case PARTIALLY_PAID -> "Qisman to'langan".toUpperCase();
                        } : "",
                monthlyFee.getCreatedAt() != null ? monthlyFee.getCreatedAt().toString() : "",
                monthlyFee.getUpdatedAt() != null ? monthlyFee.getUpdatedAt().toString() : "",
                createUser != null ? createUser.getFullName() : "",
                updateUser != null ? updateUser.getFullName() : "",
                monthlyFee.getDeleted() != null && monthlyFee.getDeleted() ? "Ha" : "Yoq"
        );

    }

    @Override
    public void updateEmployeeMonthlyFee(MonthlyFee monthlyFee) {

        try {
            String sheetName = getSheetName();
            String searchRange = sheetName + "!A2:P";

            ValueRange existingData = this.sheetService
                    .spreadsheets()
                    .values()
                    .get(studentTransactionsSheetId, searchRange)
                    .execute();
            List<List<Object>> values = existingData.getValues();
            if (values == null || values.isEmpty()) {
                log.info("No data found in the sheet to update.");
                return;
            }
            int actualRowNumber = -1; // The actual row number in the sheet (1-based)

            // Search for the expense ID
            for (int i = 0; i < values.size(); i++) {
                List<Object> row = values.get(i);
                if (row != null && !row.isEmpty()) {
                    String cellId = row.get(0).toString().trim();
                    if (!cellId.isEmpty() && cellId.equals(String.valueOf(monthlyFee.getId()))) {
                        actualRowNumber = i + 2; // +2 because: +1 for 1-based indexing, +1 for header row
                        break;
                    }
                }
            }

            // If not found, add as new expense
            if (actualRowNumber == -1) {
                log.info("Expense ID {} not found in sheet, adding as new expense", monthlyFee.getId());
                recordEmployeeMonthlyFee(monthlyFee);
                return;
            }
            List<Object> updatedRow = toRow(monthlyFee);
            ValueRange body = new ValueRange().setValues(Collections.singletonList(updatedRow));

            String updateRange = sheetName + "!A" + actualRowNumber + ":P" + actualRowNumber;

            this.sheetService
                    .spreadsheets()
                    .values()
                    .update(studentTransactionsSheetId, updateRange, body)
                    .setValueInputOption("USER_ENTERED")
                    .execute();
            log.info("Transaction with ID {} updated successfully in the sheet.", monthlyFee.getId());
        } catch (IOException e) {
            log.info("Error happened while updating student transaction in Google Sheet: {}", e.getMessage());
        }
    }

    private String getSheetName() {
        return this.monthlyFeeRange.split("!")[0];
    }
}
