package org.example.moliyaapp.service.impl;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.moliyaapp.entity.TeacherContract;
import org.example.moliyaapp.entity.Transaction;
import org.example.moliyaapp.entity.User;
import org.example.moliyaapp.enums.SalaryType;
import org.example.moliyaapp.repository.TeacherContractRepository;
import org.example.moliyaapp.repository.UserRepository;
import org.example.moliyaapp.service.EmployeeContractGoogleSheet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class EmployeeContractGoogleSheetImpl implements EmployeeContractGoogleSheet {

    private final UserRepository userRepository;
    private final TeacherContractRepository teacherContractRepository;
    private final Sheets sheetService;

    @Value("${google.sheets.employee.contracts.sheet.id}")
    private String studentTransactionsSheetId;

    @Value("${google.sheets.employee-contract.range}")
    private String employee_contractRange;


    @Override
    public void recordEmployeeContract(TeacherContract teacherContract) {

        Long createdBy = teacherContract.getCreatedBy();
        Long updatedBy = teacherContract.getUpdatedBy();
        User createUser = this.userRepository.findByIdAndDeletedFalse(createdBy)
                .orElse(null);
        User updateUser = this.userRepository.findByIdAndDeletedFalse(updatedBy)
                .orElse(null);
        try {
            List<Object> row = Arrays.asList(
                    teacherContract.getId(),
                    teacherContract.getTeacher() != null && teacherContract.getTeacher().getFullName() != null ? teacherContract.getTeacher().getFullName() : "",
                    teacherContract.getTeacher() != null && teacherContract.getTeacher().getId() != null ? teacherContract.getTeacher().getId() : "",
                    teacherContract.getSalaryType() != null ?
                            switch (teacherContract.getSalaryType()) {
                                case PRICE_PER_DAY -> "KUNLIK";
                                case PRICE_PER_LESSON -> "DARSLIK";
                                case PRICE_PER_MONTH -> "OYLIK";
                            } : "",
                    teacherContract.getMonthlySalaryOrPerLessonOrPerDay() != null ? teacherContract.getMonthlySalaryOrPerLessonOrPerDay() : 0.0,
                    teacherContract.getStartDate() != null ? teacherContract.getStartDate().toString() : "",
                    teacherContract.getEndDate() != null ? teacherContract.getEndDate().toString() : "",
                    teacherContract.getActive() != null ? teacherContract.getActive().toString() : "",
                    teacherContract.getCreatedAt() != null ? teacherContract.getCreatedAt().toString() : "",
                    teacherContract.getUpdatedAt() != null ? teacherContract.getUpdatedAt().toString() : "",
                    createUser != null ? createUser.getFullName() : "",
                    updateUser != null ? updateUser.getFullName() : "",
                    teacherContract.getDeleted() != null && teacherContract.getDeleted() ? "Ha" : "Yoq"

            );
            ValueRange valueRange = new ValueRange()
                    .setValues(Collections.singletonList(row));
            this.sheetService
                    .spreadsheets()
                    .values()
                    .append(studentTransactionsSheetId, employee_contractRange, valueRange)
                    .setValueInputOption("USER_ENTERED")
                    .setInsertDataOption("INSERT_ROWS")
                    .execute();

        } catch (IOException e) {
            log.info("Error happened while recording student transaction to Google Sheet: {}", e.getMessage());

        }
    }


    //initialize google sheet with header if not exists
    @Override
    public void initializeSheet() {
        try {
            ValueRange headerRange = this.sheetService
                    .spreadsheets()
                    .values()
                    .get(studentTransactionsSheetId, employee_contractRange)
                    .execute();
            List<List<Object>> values = headerRange.getValues();
            if (values == null || values.isEmpty()) {
                List<Object> header = Arrays.asList(
                        "Shartnoma ID",
                        "Xodim ismi",
                        "Xodim ID",
                        "Maosh turi",
                        "Maosh miqdori",
                        "Shartnoma boshlanish sanasi",
                        "Shartnoma tugash sanasi",
                        "Shartnoma holati",
                        "Yaratilgan sana",
                        "Oxirgi yangilangan sana",
                        "Yaratgan foydalanuvchi",
                        "Oxirgi yangilagan foydalanuvchi",
                        "Ochirilgan"
                );
                ValueRange headerValueRange = new ValueRange()
                        .setValues(Collections.singletonList(header));
                this.sheetService
                        .spreadsheets()
                        .values()
                        .append(studentTransactionsSheetId, employee_contractRange, headerValueRange)
                        .setValueInputOption("USER_ENTERED")
                        .setInsertDataOption("INSERT_ROWS")
                        .execute();
                this.bulkyAllEmployeeContract();
            }
        } catch (IOException e) {
            log.error("Error initializing Google Sheet: {}", e.getMessage());
        }
    }

    private void bulkyAllEmployeeContract() throws IOException {
        List<TeacherContract> list = this.teacherContractRepository.findAll();
        if (list.isEmpty()) {
            log.info("Shartnomalar topilmadi!");
            return;
        }
        List<List<Object>> rows = new ArrayList<>();
        for (TeacherContract transaction : list) {
            rows.add(toRow(transaction));
        }
        ValueRange body = new ValueRange().setValues(rows);
        sheetService.spreadsheets().values()
                .append(studentTransactionsSheetId, employee_contractRange, body)
                .setValueInputOption("USER_ENTERED")
                .setInsertDataOption("INSERT_ROWS")
                .execute();
    }

    private List<Object> toRow(TeacherContract teacherContract) {
        Long createdBy = teacherContract.getCreatedBy();
        Long updatedBy = teacherContract.getUpdatedBy();
        User createUser = this.userRepository.findByIdAndDeletedFalse(createdBy)
                .orElse(null);
        User updateUser = this.userRepository.findByIdAndDeletedFalse(updatedBy)
                .orElse(null);
        return Arrays.asList(
                teacherContract.getId(),
                teacherContract.getTeacher() != null && teacherContract.getTeacher().getFullName() != null ? teacherContract.getTeacher().getFullName() : "",
                teacherContract.getTeacher() != null && teacherContract.getTeacher().getId() != null ? teacherContract.getTeacher().getId() : "",
                teacherContract.getSalaryType() != null ?
                        switch (teacherContract.getSalaryType()) {
                            case PRICE_PER_DAY -> "KUNLIK";
                            case PRICE_PER_LESSON -> "DARSLIK";
                            case PRICE_PER_MONTH -> "OYLIK";
                        } : "",
                teacherContract.getMonthlySalaryOrPerLessonOrPerDay() != null ? teacherContract.getMonthlySalaryOrPerLessonOrPerDay() : 0.0,
                teacherContract.getStartDate() != null ? teacherContract.getStartDate().toString() : "",
                teacherContract.getEndDate() != null ? teacherContract.getEndDate().toString() : "",
                teacherContract.getActive() != null ? teacherContract.getActive().toString() : "",
                teacherContract.getCreatedAt() != null ? teacherContract.getCreatedAt().toString() : "",
                teacherContract.getUpdatedAt() != null ? teacherContract.getUpdatedAt().toString() : "",
                createUser != null ? createUser.getFullName() : "",
                updateUser != null ? updateUser.getFullName() : "",
                teacherContract.getDeleted() != null && teacherContract.getDeleted() ? "Ha" : "Yoq"
        );
    }

    //need to write update
    @Override
    public void updateEmployeeContract(TeacherContract teacherContract) {

        try {
            String sheetName = getSheetName();
            String searchRange = sheetName + "!A2:M";

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
                    if (!cellId.isEmpty() && cellId.equals(String.valueOf(teacherContract.getId()))) {
                        actualRowNumber = i + 2; // +2 because: +1 for 1-based indexing, +1 for header row
                        break;
                    }
                }
            }

            // If not found, add as new expense
            if (actualRowNumber == -1) {
                log.info("Expense ID {} not found in sheet, adding as new expense", teacherContract.getId());
                recordEmployeeContract(teacherContract);
                return;
            }
            List<Object> updatedRow = toRow(teacherContract);
            ValueRange body = new ValueRange().setValues(Collections.singletonList(updatedRow));

            String updateRange = sheetName + "!A2" + actualRowNumber + ":M" + actualRowNumber;

            this.sheetService
                    .spreadsheets()
                    .values()
                    .update(studentTransactionsSheetId, updateRange, body)
                    .setValueInputOption("USER_ENTERED")
                    .execute();
        } catch (IOException e) {
            log.error("Error initializing Google Sheet: {}", e.getMessage());
        }
    }

    private String getSheetName() {
        return this.employee_contractRange.split("!")[0];
    }
}
