package org.example.moliyaapp.service.impl;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.AppendValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.moliyaapp.entity.Expenses;
import org.example.moliyaapp.entity.StudentContract;
import org.example.moliyaapp.entity.User;
import org.example.moliyaapp.repository.ExpensesRepository;
import org.example.moliyaapp.repository.UserRepository;
import org.example.moliyaapp.service.ExpenseGoogleSheet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Primary
@RequiredArgsConstructor
@Service
@Slf4j
public class ExpenseGoogleSheetImpl implements ExpenseGoogleSheet {

    private final Sheets sheetService;
    private final ExpensesRepository expensesRepository;
    private final UserRepository userRepository;

    @Value("${google.sheets.employee.contracts.sheet.id}")
    private String studentContractsSheetId;

    @Value("${google.sheets.expense.range}")
    private String expenseRange;


    public void recordExpense(Expenses expenses) {
        Long createdBy = expenses.getCreatedBy();
        Long updatedBy = expenses.getUpdatedBy();

        User createUser = this.userRepository.findByIdAndDeletedFalse(createdBy)
                .orElse(null);

        User updateUser = this.userRepository.findByIdAndDeletedFalse(updatedBy)
                .orElse(null);
        try {
            List<Object> row = Arrays.asList(
                    expenses.getId(),
                    expenses.getName() != null ? expenses.getName() : "",
                    expenses.getAmount() != null ? expenses.getAmount() : 0.0,
                    expenses.getDescription() != null ? expenses.getDescription() : "",
                    expenses.getPaymentType() != null ? expenses.getPaymentType().toString() : "",
                    expenses.getTransactionType() != null ? expenses.getTransactionType().toString() : "",
                    expenses.getReceiver() != null ? expenses.getReceiver() : "",
                    expenses.getSpender() != null ? expenses.getSpender() : "",
                    expenses.getCategory() != null ? expenses.getCategory().getName() : "",
                    expenses.getCreatedAt() != null ? expenses.getCreatedAt().toString() : "",
                    expenses.getUpdatedAt() != null ? expenses.getUpdatedAt().toString() : "",
                    createUser != null ? createUser.getFullName() : "",
                    updateUser != null ? updateUser.getFullName() : "",
                    expenses.getDeleted() != null && expenses.getDeleted() ? "Ha" : "Yoq"
            );

            ValueRange valueRange = new ValueRange()
                    .setValues(Collections.singletonList(row));


            this.sheetService
                    .spreadsheets().values()
                    .append(studentContractsSheetId, expenseRange, valueRange)
                    .setValueInputOption("USER_ENTERED")
                    .setInsertDataOption("INSERT_ROWS")
                    .execute();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void initializeSheet() {
        try {
            ValueRange headerRange = this.sheetService
                    .spreadsheets()
                    .values()
                    .get(studentContractsSheetId, expenseRange)
                    .execute();

            if (headerRange.getValues() == null || headerRange.getValues().isEmpty()) {

                List<Object> headers = Arrays.asList(
                        "Xarajat ID", "Xarajat ismi", "Miqdor",
                        "Izoh", "To'lov turi", "Transaksiya turi",
                        "Qabul qiluvchi", "Berivchi", "Kategoriya ismi",
                        "Qoshilgan vaqt", "Tahrirlangan vaqt", "Kim qo'shdi!?", "Kim tahrirladi!?",
                        "O'chirilganmi?"

                );
                ValueRange headerBody = new ValueRange()
                        .setValues(Collections.singletonList(headers));

                sheetService.spreadsheets().values()
                        .update(studentContractsSheetId, expenseRange, headerBody)
                        .setValueInputOption("USER_ENTERED")
                        .execute();

                this.bulkExportAllExpenses();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void bulkExportAllExpenses() throws IOException {
        List<Expenses> list = this.expensesRepository.findAll();
        if (list.isEmpty()) {
            log.info("xarajatlar topilmadi!");
            return;
        }
        List<List<Object>> rows = new ArrayList<>();
        for (Expenses c : list) {
            rows.add(toRow(c));
        }
        ValueRange body = new ValueRange().setValues(rows);
        sheetService.spreadsheets().values()
                .append(studentContractsSheetId, expenseRange, body)
                .setValueInputOption("USER_ENTERED")
                .setInsertDataOption("INSERT_ROWS")
                .execute();
    }

    private List<Object> toRow(Expenses expenses) {
        Long createdBy = expenses.getCreatedBy();
        Long updatedBy = expenses.getUpdatedBy();

        User createUser = this.userRepository.findByIdAndDeletedFalse(createdBy)
                .orElse(null);

        User updateUser = this.userRepository.findByIdAndDeletedFalse(updatedBy)
                .orElse(null);
        List<Object> row = new ArrayList<>();
        row.add(expenses.getId());
        row.add(expenses.getName() != null ? expenses.getName() : "");
        row.add(expenses.getAmount() != null ? expenses.getAmount() : 0.0);
        row.add(expenses.getDescription() != null ? expenses.getDescription() : "");
        row.add(expenses.getPaymentType() != null ? expenses.getPaymentType().toString() : "");
        row.add(expenses.getTransactionType() != null ? expenses.getTransactionType().toString() : "");
        row.add(expenses.getReceiver() != null ? expenses.getReceiver() : "");
        row.add(expenses.getSpender() != null ? expenses.getSpender() : "");
        row.add(expenses.getCategory() != null ? expenses.getCategory().getName() : "");
        row.add(expenses.getCreatedAt() != null ? expenses.getCreatedAt().toString() : "");
        row.add(expenses.getUpdatedAt() != null ? expenses.getUpdatedAt().toString() : "");
        row.add(createUser != null ? createUser.getFullName() : "");
        row.add(updateUser != null ? updateUser.getFullName() : "");
        row.add(expenses.getDeleted() != null && expenses.getDeleted() ? "Ha" : "Yoq");
        return row;
    }

    public void updateExpense(Expenses expenses) {
        try {
            // Get the sheet name dynamically
            String sheetName = getSheetName();

            // Search in data rows only (starting from row 2)
            String searchRange = sheetName + "!A2:N";

            ValueRange response = sheetService
                    .spreadsheets()
                    .values()
                    .get(studentContractsSheetId, searchRange)
                    .execute();

            List<List<Object>> values = response.getValues();
            if (values == null || values.isEmpty()) {
                log.info("No data found in sheet, adding as new expense");
                recordExpense(expenses);
                return;
            }

            int actualRowNumber = -1; // The actual row number in the sheet (1-based)

            // Search for the expense ID
            for (int i = 0; i < values.size(); i++) {
                List<Object> row = values.get(i);
                if (row != null && !row.isEmpty()) {
                    String cellId = row.get(0).toString().trim();
                    if (!cellId.isEmpty() && cellId.equals(String.valueOf(expenses.getId()))) {
                        actualRowNumber = i + 2; // +2 because: +1 for 1-based indexing, +1 for header row
                        break;
                    }
                }
            }

            // If not found, add as new expense
            if (actualRowNumber == -1) {
                log.info("Expense ID {} not found in sheet, adding as new expense", expenses.getId());
                recordExpense(expenses);
                return;
            }

            // Prepare updated row data
            List<Object> updatedRow = toRow(expenses);
            ValueRange body = new ValueRange().setValues(Collections.singletonList(updatedRow));

            // Update the specific row
            String updateRange = sheetName + "!A" + actualRowNumber + ":N" + actualRowNumber;

            this.sheetService
                    .spreadsheets()
                    .values()
                    .update(studentContractsSheetId, updateRange, body)
                    .setValueInputOption("USER_ENTERED")
                    .execute();

            log.info("Successfully updated expense ID {} at row {}", expenses.getId(), actualRowNumber);

        } catch (IOException e) {
            log.error("Failed to update expense ID {} in Google Sheet", expenses.getId(), e);
            throw new RuntimeException("Failed to update expense in Google Sheets", e);
        }
    }

    // Helper method to get sheet name (add this if not already present)
    private String getSheetName() {
        return expenseRange.split("!")[0];
    }

}
