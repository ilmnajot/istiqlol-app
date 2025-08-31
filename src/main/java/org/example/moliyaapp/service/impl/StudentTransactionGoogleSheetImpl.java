package org.example.moliyaapp.service.impl;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import lombok.extern.slf4j.Slf4j;
import org.example.moliyaapp.entity.MonthlyFee;
import org.example.moliyaapp.entity.StudentContract;
import org.example.moliyaapp.entity.Transaction;
import org.example.moliyaapp.entity.User;
import org.example.moliyaapp.enums.Months;
import org.example.moliyaapp.repository.MonthlyFeeRepository;
import org.example.moliyaapp.repository.TransactionRepository;
import org.example.moliyaapp.repository.UserRepository;
import org.example.moliyaapp.service.StudentTransactionGoogleSheet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.example.moliyaapp.utils.GoogleSheetConstants.INSERT_ROWS;
import static org.example.moliyaapp.utils.GoogleSheetConstants.USER_ENTERED;

@Slf4j
@Service
public class StudentTransactionGoogleSheetImpl implements StudentTransactionGoogleSheet {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final Sheets sheetService;


    @Value("${google.sheets.student.contracts.sheet.id}")
    private String studentTransactionsSheetId;

    @Value("${google.sheets.student-transaction.range}")
    private String studentTransactionsRange;

    public StudentTransactionGoogleSheetImpl(TransactionRepository transactionRepository, MonthlyFeeRepository monthlyFeeRepository, UserRepository userRepository, Sheets sheetService) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.sheetService = sheetService;

    }


    public void recordStudentTransactions(Transaction transaction) {
        MonthlyFee fee = transaction.getMonthlyFee();
        StudentContract studentContract = fee != null && fee.getStudentContract() != null
                ? fee.getStudentContract()
                : null;
        Months months = fee != null ? fee.getMonths() : null;
        Long studentContractId = studentContract != null ? studentContract.getId() : null;
        String studentFullName = studentContract != null ? studentContract.getStudentFullName() : null;
        String academicYear = studentContract != null ? studentContract.getAcademicYear() : null;


        Long createdBy = transaction.getCreatedBy();
        Long updatedBy = transaction.getUpdatedBy();
        User createUser = this.userRepository.findByIdAndDeletedFalse(createdBy)
                .orElse(null);
        User updateUser = this.userRepository.findByIdAndDeletedFalse(updatedBy)
                .orElse(null);
        try {
            List<Object> row = Arrays.asList(
                    transaction.getId(),
                    studentContractId != null ? studentContractId : "",
                    studentFullName != null ? studentFullName : "",
                    academicYear != null ? academicYear : "",
                    months != null ? months.toString() : "",
                    transaction.getAmount() != null ? transaction.getAmount() : 0.0,
                    transaction.getTransactionType() != null ? transaction.getTransactionType().toString() : "",
                    transaction.getPaymentType() != null ? transaction.getPaymentType().toString() : "",
                    transaction.getDescription() != null ? transaction.getDescription() : "",
                    transaction.getCreatedAt() != null ? transaction.getCreatedAt().toString() : "",
                    transaction.getUpdatedAt() != null ? transaction.getUpdatedAt().toString() : "",
                    createUser != null ? createUser.getFullName() : "",
                    updateUser != null ? updateUser.getFullName() : "",
                    transaction.getDeleted() != null && transaction.getDeleted() ? "Ha" : "Yoq"
            );
            ValueRange valueRange = new ValueRange()
                    .setValues(Collections.singletonList(row));
            this.sheetService
                    .spreadsheets()
                    .values()
                    .append(studentTransactionsSheetId, studentTransactionsRange, valueRange)
                    .setValueInputOption(USER_ENTERED)
                    .setInsertDataOption(INSERT_ROWS)
                    .execute();

        } catch (IOException e) {
            log.info("Error happened while recording student transaction to Google Sheet: {}", e.getMessage());

        }

    }

    public void initializeSheet() {
        try {
            ValueRange headerRange = this.sheetService
                    .spreadsheets()
                    .values()
                    .get(studentTransactionsSheetId, studentTransactionsRange)
                    .execute();
            if (headerRange.getValues() == null || headerRange.getValues().isEmpty()) {
                List<Object> header = Arrays.asList(
                        "Transaksiya ID",
                        "O'quvchi Sh. ID",
                        "Student Full Name",
                        "Academic yil",
                        "Oy",
                        "Miqdor",
                        "Transaksiya Turi",
                        "Tolov Turi",
                        "Izoh",
                        "Kiritilgan Vaqt",
                        "Tahrirlangan Vaqt",
                        "Kim kiritgan!?",
                        "Kim tahrirlagan!?",
                        "O'chirilganmi?"
                );
                ValueRange headerValueRange = new ValueRange()
                        .setValues(Collections.singletonList(header));
                this.sheetService
                        .spreadsheets()
                        .values()
                        .update(studentTransactionsSheetId, studentTransactionsRange, headerValueRange)
                        .setValueInputOption(USER_ENTERED)
                        .execute();

                this.bulkExportAllStudentTransactions();
            }
        } catch (IOException e) {
            log.info("Error happened while initializing Google Sheet: {}", e.getMessage());
        }
    }

    private void bulkExportAllStudentTransactions() throws IOException {
        List<Transaction> transactions = this.transactionRepository.findAllByExp();
        if (transactions.isEmpty()) {
            log.info("Transaksiyalar topilmadi!");
            return;
        }
        List<List<Object>> rows = new ArrayList<>();
        for (Transaction transaction : transactions) {
            rows.add(toRow(transaction));
        }
        ValueRange body = new ValueRange().setValues(rows);
        sheetService.spreadsheets().values()
                .append(studentTransactionsSheetId, studentTransactionsRange, body)
                .setValueInputOption(USER_ENTERED)
                .setInsertDataOption(INSERT_ROWS)
                .execute();
        log.info("Bulk export of student transactions completed. Total records exported: {}", transactions.size());
    }

    private List<Object> toRow(Transaction transaction) {
        Long createdBy = transaction.getCreatedBy();
        Long updatedBy = transaction.getUpdatedBy();
        User createUser = this.userRepository.findByIdAndDeletedFalse(createdBy)
                .orElse(null);
        User updateUser = this.userRepository.findByIdAndDeletedFalse(updatedBy)
                .orElse(null);


        MonthlyFee fee = transaction.getMonthlyFee();
        StudentContract studentContract = fee != null && fee.getStudentContract() != null
                ? fee.getStudentContract()
                : null;
        Months months = fee != null ? fee.getMonths() : null;
        String studentContractId = studentContract != null ? studentContract.getUniqueId() : null;
        String studentFullName = studentContract != null ? studentContract.getStudentFullName() : null;
        String academicYear = studentContract != null ? studentContract.getAcademicYear() : null;

        return Arrays.asList(
                transaction.getId(),
                studentContractId != null ? studentContractId : "",
                studentFullName != null ? studentFullName : "",
                academicYear != null ? academicYear : "",
                months != null ? months.toString() : "",
                transaction.getAmount() != null ? transaction.getAmount() : 0.0,
                transaction.getTransactionType() != null ? transaction.getTransactionType().toString() : "",
                transaction.getPaymentType() != null ? transaction.getPaymentType().toString() : "",
                transaction.getDescription() != null ? transaction.getDescription() : "",
                transaction.getCreatedAt() != null ? transaction.getCreatedAt().toString() : "",
                transaction.getUpdatedAt() != null ? transaction.getUpdatedAt().toString() : "",
                createUser != null ? createUser.getFullName() : "",
                updateUser != null ? updateUser.getFullName() : "",
                transaction.getDeleted() != null && transaction.getDeleted() ? "Ha" : "Yoq"
        );
    }

    //update student transaction in google sheet
    public void updateStudentTransaction(Transaction transaction) {
        try {

            String sheetName = getSheetName();
            String searchRange = sheetName + "!A2:N";

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
                    if (!cellId.isEmpty() && cellId.equals(String.valueOf(transaction.getId()))) {
                        actualRowNumber = i + 2; // +2 because: +1 for 1-based indexing, +1 for header row
                        break;
                    }
                }
            }

            // If not found, add as new expense
            if (actualRowNumber == -1) {
                log.info("Expense ID {} not found in sheet, adding as new expense", transaction.getId());
                recordStudentTransactions(transaction);
                return;
            }

            List<Object> updatedRow = toRow(transaction);
            ValueRange body = new ValueRange().setValues(Collections.singletonList(updatedRow));

            String updateRange = sheetName + "!A2" + actualRowNumber + ":N" + actualRowNumber;

            this.sheetService
                    .spreadsheets()
                    .values()
                    .update(studentTransactionsSheetId, updateRange, body)
                    .setValueInputOption(USER_ENTERED)
                    .execute();
            log.info("Transaction with ID {} updated successfully in the sheet.", transaction.getId());
        } catch (IOException e) {
            log.info("Error happened while updating student transaction in Google Sheet: {}", e.getMessage());
        }
    }

    private String getSheetName() {
        return this.studentTransactionsRange.split("!")[0];
    }
}
