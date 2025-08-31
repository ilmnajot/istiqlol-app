package org.example.moliyaapp.service.impl;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.moliyaapp.entity.User;
import org.example.moliyaapp.entity.UserRole;
import org.example.moliyaapp.repository.MonthlyFeeRepository;
import org.example.moliyaapp.repository.UserRepository;
import org.example.moliyaapp.service.EmployeeGoogleSheet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeGoogleSheetImpl implements EmployeeGoogleSheet {

    @Value("${google.sheets.employee.contracts.sheet.id}")
    private String studentTransactionsSheetId;

    @Value("${google.sheets.employee.range}")
    private String employeeRange;


    private final UserRepository userRepository;
    private final Sheets sheetsService;
    private final MonthlyFeeRepository monthlyFeeRepository;

    //record all employees in google sheet
    public void recordEmployee(User user) {
        Long createdBy = user.getCreatedBy();
        Long updatedBy = user.getUpdatedBy();
        User createUser = this.userRepository.findByIdAndDeletedFalse(createdBy)
                .orElse(null);
        User updateUser = this.userRepository.findByIdAndDeletedFalse(updatedBy)
                .orElse(null);

        Set<UserRole> roles = user.getRole();
        String roleName = "";
        for (UserRole role : roles) {
            roleName = role.getName();
        }

        try {
            List<Object> row = Arrays.asList(
                    user.getId(),
                    user.getFullName() != null ? user.getFullName() : "",
                    user.getEmail() != null ? user.getEmail() : "",
                    user.getPhoneNumber() != null ? user.getPhoneNumber() : "",
                    user.getStatus() != null ? user.getStatus().toString() : "",
                    user.getContractNumber() != null ? user.getContractNumber() : "",
                    roleName != null ? roleName : "",
                    user.getCreatedAt() != null ? user.getCreatedAt().toString() : "",
                    user.getUpdatedAt() != null ? user.getUpdatedAt().toString() : "",
                    createUser != null ? createUser.getFullName() : "",
                    updateUser != null ? updateUser.getFullName() : "",
                    user.getDeleted() != null && user.getDeleted() ? "Ha" : "Yoq"
            );
            ValueRange valueRange = new ValueRange()
                    .setValues(Collections.singletonList(row));
            this.sheetsService
                    .spreadsheets()
                    .values()
                    .append(studentTransactionsSheetId, employeeRange, valueRange)
                    .setValueInputOption("USER_ENTERED")
                    .setInsertDataOption("INSERT_ROWS")
                    .execute();

        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public void initializeSheet() {
        try {
            ValueRange headerRange = this.sheetsService
                    .spreadsheets()
                    .values()
                    .get(studentTransactionsSheetId, employeeRange)
                    .execute();
            if (headerRange == null || headerRange.getValues() == null || headerRange.getValues().isEmpty()) {
                List<Object> row = Arrays.asList(
                        "Xodim ID",
                        "Xodim F.I.SH",
                        "E-Pochta",
                        "Telefon raqami",
                        "Holati",
                        "Shartnoma raqami",
                        "Role",
                        "Yaratilgan sana",
                        "O'zgartirilgan sana",
                        "Yaratgan vaqt",
                        "Tahrirlanga vaqt",
                        "O'chirilgan"

                );

                ValueRange valueRange = new ValueRange()
                        .setValues(Collections.singletonList(row));

                this.sheetsService
                        .spreadsheets()
                        .values()
                        .update(studentTransactionsSheetId, employeeRange, valueRange)
                        .setValueInputOption("USER_ENTERED")
                        .execute();

                this.bulkyAllEmployee();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void bulkyAllEmployee() {
        List<User> list = this.userRepository.findAll();
        if (list.isEmpty()) {
            log.info("xodimlar topilmadi!");
            return;
        }
        List<List<Object>> rows = new ArrayList<>();
        for (User user : list) {
            rows.add(toRow(user));
        }
        ValueRange body = new ValueRange().setValues(rows);
        try {
            sheetsService.spreadsheets().values()
                    .append(studentTransactionsSheetId, employeeRange, body)
                    .setValueInputOption("USER_ENTERED")
                    .setInsertDataOption("INSERT_ROWS")
                    .execute();
        } catch (IOException e) {
            log.info("Barcha xodimlar google sheet ga yozilmadi! jami: {}", list.size());

        }
        log.info("Bulk export of employees  completed. Total records exported: {}", list.size());
    }

    private List<Object> toRow(User user) {
        Long createdBy = user.getCreatedBy();
        Long updatedBy = user.getUpdatedBy();
        User createUser = this.userRepository.findByIdAndDeletedFalse(createdBy)
                .orElse(null);
        User updateUser = this.userRepository.findByIdAndDeletedFalse(updatedBy)
                .orElse(null);
        Set<UserRole> roles = user.getRole();
        String roleName = "";
        for (UserRole role : roles) {
            roleName = role.getName();
        }

        return Arrays.asList(
                user.getId(),
                user.getFullName() != null ? user.getFullName() : "",
                user.getEmail() != null ? user.getEmail() : "",
                user.getPhoneNumber() != null ? user.getPhoneNumber() : "",
                user.getStatus() != null ? user.getStatus().toString() : "",
                user.getContractNumber() != null ? user.getContractNumber() : "",
                roleName != null ? roleName : "",
                user.getCreatedAt() != null ? user.getCreatedAt().toString() : "",
                user.getUpdatedAt() != null ? user.getUpdatedAt().toString() : "",
                createUser != null ? createUser.getFullName() : "",
                updateUser != null ? updateUser.getFullName() : "",
                user.getDeleted() != null && user.getDeleted() ? "Ha" : "Yoq"
        );

    }

    public void updateEmployee(User user) {
        try {
            String sheetName = getSheetName();
            String searchRange = sheetName + "!A2:L";

            ValueRange existingData = this.sheetsService
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
                    if (!cellId.isEmpty() && cellId.equals(String.valueOf(user.getId()))) {
                        actualRowNumber = i + 2; // +2 because: +1 for 1-based indexing, +1 for header row
                        break;
                    }
                }
            }
            // If not found, add as new expense
            if (actualRowNumber == -1) {
                log.info("Expense ID {} not found in sheet, adding as new expense", user.getId());
                this.recordEmployee(user);
                return;
            }
            List<Object> updatedRow = toRow(user);
            ValueRange body = new ValueRange().setValues(Collections.singletonList(updatedRow));

            String updateRange = sheetName + "!A2" + actualRowNumber + ":L" + actualRowNumber;

            this.sheetsService
                    .spreadsheets()
                    .values()
                    .update(studentTransactionsSheetId, updateRange, body)
                    .setValueInputOption("USER_ENTERED")
                    .execute();
            log.info("Transaction with ID {} updated successfully in the sheet.", user.getId());
        } catch (IOException e) {
            log.info("Error happened while updating student transaction in Google Sheet: {}", e.getMessage());
        }
    }

    private String getSheetName() {
        return this.employeeRange.split("!")[0];
    }

}
