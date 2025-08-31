package org.example.moliyaapp.service.impl;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.moliyaapp.entity.TeacherTable;
import org.example.moliyaapp.entity.User;
import org.example.moliyaapp.enums.Role;
import org.example.moliyaapp.repository.TeacherTableRepository;
import org.example.moliyaapp.repository.UserRepository;
import org.example.moliyaapp.service.EmployeeTabelSheet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

import static org.example.moliyaapp.enums.Role.EDUCATIONAL_DEPARTMENT;

@Slf4j
@RequiredArgsConstructor
@Service
public class EmployeeTabelSheetImpl implements EmployeeTabelSheet {

    private final TeacherTableRepository teacherTableRepository;
    private final Sheets sheetService;
    private final UserRepository userRepository;


    @Value("${google.sheets.employee.contracts.sheet.id}")
    private String studentTransactionsSheetId;

    @Value("${google.sheets.employee-tabel.range}")
    private String employee_tabelRange;

    @Override
    public void recordEmployeeTabel(TeacherTable teacherTable) {
        Long createdBy = teacherTable.getCreatedBy();
        Long updatedBy = teacherTable.getUpdatedBy();
        User createUser = this.userRepository.findByIdAndDeletedFalse(createdBy)
                .orElse(null);
        User updateUser = this.userRepository.findByIdAndDeletedFalse(updatedBy)
                .orElse(null);
        try {
            List<Object> row = Arrays.asList(
                    teacherTable.getId(),
                    teacherTable.getTeacher() != null ? teacherTable.getTeacher().getFullName() : "",
                    Objects.requireNonNull(teacherTable.getTeacher()).getRole() != null &&
                            !teacherTable.getTeacher().getRole().isEmpty() ?
                            convertRoleName(teacherTable.getTeacher().getRole().stream().findFirst().get().getName()) : "",
                    teacherTable.getMonths() != null ? teacherTable.getMonths().name() : "",
                    teacherTable.getWorkDaysOrLessons() != null ? teacherTable.getWorkDaysOrLessons() : 0,
                    teacherTable.getWorkedDaysOrLessons() != null ? teacherTable.getWorkedDaysOrLessons() : 0,
                    teacherTable.getExtraWorkedDaysOrLessons() != null ? teacherTable.getExtraWorkedDaysOrLessons() : 0,
                    teacherTable.getMissedWorkDaysOrLessons() != null ? teacherTable.getMissedWorkDaysOrLessons() : 0,
                    teacherTable.getMonthlySalary() != null ? teacherTable.getMonthlySalary() : 0.0,
                    teacherTable.getTeacherContract() != null && teacherTable.getTeacherContract()!= null ?
                            switch (teacherTable.getTeacherContract().getSalaryType()) {
                                case PRICE_PER_DAY -> "KUNLIK";
                                case PRICE_PER_LESSON -> "DARSLIK";
                                case PRICE_PER_MONTH -> "OYLIK";
                            }:"",
                    teacherTable.getAmount() != null ? teacherTable.getAmount() : 0.0,
                    teacherTable.getDescription() != null ? teacherTable.getDescription() : "",
                    teacherTable.getCreatedAt() != null ? teacherTable.getCreatedAt().toString() : "",
                    teacherTable.getUpdatedAt() != null ? teacherTable.getUpdatedAt().toString() : "",
                    createUser != null ? createUser.getFullName() : "",
                    updateUser != null ? updateUser.getFullName() : "",
                    teacherTable.getDeleted() != null && teacherTable.getDeleted() ? "Ha" : "Yoq"
            );
            this.sheetService
                    .spreadsheets()
                    .values()
                    .append(studentTransactionsSheetId, employee_tabelRange, new com.google.api.services.sheets.v4.model.ValueRange().setValues(List.of(row)))
                    .setValueInputOption("USER_ENTERED")
                    .setInsertDataOption("INSERT_ROWS")
                    .execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private String convertRoleName(String roleName) {
        if (roleName == null) {
            return "";
        }
        return switch (roleName.toUpperCase()) {
            case "HR" -> "HR";
            case "PRINCIPAL" -> "Direktor";
            case "ADMIN" -> "ADMIN | MOLIYA";
            case "OWNER" -> "Tasischi";
            case "TEACHER" -> "O'qituvchi";
            case "CASHIER" -> "Kassir";
            case "EMPLOYEE" -> "Xodim";
            case "DEVELOPER" -> "Dasturchi";
            case "MARKETING" -> "Marketing";
            case "RECEPTION" -> "Qabul bo'limi";
            case "EDUCATIONAL_DEPARTMENT" -> "O'quv bo'limi";
            default -> "";

        };
    }

    @Override
    public void initializeSheet() {

        try {
            ValueRange valueRange = this.sheetService
                    .spreadsheets()
                    .values()
                    .get(studentTransactionsSheetId, employee_tabelRange)
                    .execute();

            if (valueRange.getValues() == null || valueRange.getValues().isEmpty()) {
                List<Object> header = Arrays.asList(
                        "Tabel ID",
                        "Xodim F.I.SH",
                        "Xodim lavozimi",
                        "Oy",
                        "Ish kunlari/soatlari",
                        "Ishlagan kunlari/soatlari",
                        "Qo'shimcha o'tgan kun/dars",
                        "Qoldirilgan ish kunlari/soatlari",
                        "Oylik maosh",
                        "Oylik turi",
                        "Hisoblangan miqdor",
                        "Izoh",
                        "Qoshilgan vaqt",
                        "Tahrirlangan vaqt",
                        "Kim qoshdi!?",
                        "Kim tahrirladi!?",
                        "Ochirildimi!?"
                );
                ValueRange headerValueRange = new ValueRange()
                        .setValues(Collections.singletonList(header));
                this.sheetService
                        .spreadsheets()
                        .values()
                        .update(studentTransactionsSheetId, employee_tabelRange, headerValueRange)
                        .setValueInputOption("USER_ENTERED")
                        .execute();
                this.bulkExportAllStudentTransactions();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void bulkExportAllStudentTransactions() throws IOException {
        List<TeacherTable> tableList = this.teacherTableRepository.findAll();
        if (tableList.isEmpty()) {
            log.info("No teacher tabels to export.");
            return;
        }
        List<List<Object>> rows = new ArrayList<>();
        for (TeacherTable table : tableList) {
            rows.add(toRow(table));
        }
        ValueRange body = new ValueRange().setValues(rows);
        this.sheetService
                .spreadsheets()
                .values()
                .append(studentTransactionsSheetId, employee_tabelRange, body)
                .setValueInputOption("USER_ENTERED")
                .setInsertDataOption("INSERT_ROWS")
                .execute();
    }

    private List<Object> toRow(TeacherTable teacherTable) {
        Long createdBy = teacherTable.getCreatedBy();
        Long updatedBy = teacherTable.getUpdatedBy();
        User createUser = this.userRepository.findByIdAndDeletedFalse(createdBy)
                .orElse(null);
        User updateUser = this.userRepository.findByIdAndDeletedFalse(updatedBy)
                .orElse(null);
        return Arrays.asList(
                teacherTable.getId(),
                teacherTable.getTeacher() != null ? teacherTable.getTeacher().getFullName() : "",
                Objects.requireNonNull(teacherTable.getTeacher()).getRole() != null &&
                        !teacherTable.getTeacher().getRole().isEmpty() ?
                        convertRoleName(teacherTable.getTeacher().getRole().stream().findFirst().get().getName()) : "",
                teacherTable.getMonths() != null ? teacherTable.getMonths().name() : "",
                teacherTable.getWorkDaysOrLessons() != null ? teacherTable.getWorkDaysOrLessons() : 0,
                teacherTable.getWorkedDaysOrLessons() != null ? teacherTable.getWorkedDaysOrLessons() : 0,
                teacherTable.getExtraWorkedDaysOrLessons() != null ? teacherTable.getExtraWorkedDaysOrLessons() : 0,
                teacherTable.getMissedWorkDaysOrLessons() != null ? teacherTable.getMissedWorkDaysOrLessons() : 0,
                teacherTable.getMonthlySalary() != null ? teacherTable.getMonthlySalary() : 0.0,
                teacherTable.getTeacherContract() != null && teacherTable.getTeacherContract()!= null ?
                        switch (teacherTable.getTeacherContract().getSalaryType()) {
                            case PRICE_PER_DAY -> "KUNLIK";
                            case PRICE_PER_LESSON -> "DARSLIK";
                            case PRICE_PER_MONTH -> "OYLIK";
                        }:"",
                teacherTable.getAmount() != null ? teacherTable.getAmount() : 0.0,
                teacherTable.getDescription() != null ? teacherTable.getDescription() : "",
                teacherTable.getCreatedAt() != null ? teacherTable.getCreatedAt().toString() : "",
                teacherTable.getUpdatedAt() != null ? teacherTable.getUpdatedAt().toString() : "",
                createUser != null ? createUser.getFullName() : "",
                updateUser != null ? updateUser.getFullName() : "",
                teacherTable.getDeleted() != null && teacherTable.getDeleted() ? "Ha" : "Yoq"
        );
    }

    @Override
    public void updateEmployeeTabel(TeacherTable teacherTable) {
        try {
            String sheetName = getSheetName();
            String searchRange = sheetName + "!A2:Q";

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
                    if (!cellId.isEmpty() && cellId.equals(String.valueOf(teacherTable.getId()))) {
                        actualRowNumber = i + 2; // +2 because: +1 for 1-based indexing, +1 for header row
                        break;
                    }
                }
            }

            // If not found, add as new expense
            if (actualRowNumber == -1) {
                log.info("Expense ID {} not found in sheet, adding as new expense", teacherTable.getId());
                recordEmployeeTabel(teacherTable);
                return;
            }
            List<Object> updatedRow = toRow(teacherTable);
            ValueRange body = new ValueRange().setValues(Collections.singletonList(updatedRow));

            String updateRange = sheetName + "!A2" + actualRowNumber + ":Q" + actualRowNumber;

            this.sheetService
                    .spreadsheets()
                    .values()
                    .update(studentTransactionsSheetId, updateRange, body)
                    .setValueInputOption("USER_ENTERED")
                    .execute();
            log.info("Transaction with ID {} updated successfully in the sheet.", teacherTable.getId());
        } catch (IOException e) {
            log.info("Error happened while updating student transaction in Google Sheet: {}", e.getMessage());
        }

    }

    private String getSheetName() {
        return this.employee_tabelRange.split("!")[0];
    }
}
