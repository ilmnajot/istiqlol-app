package org.example.moliyaapp.service.impl;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.AppendValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.moliyaapp.entity.*;
import org.example.moliyaapp.repository.GoogleSheetsOperations;
import org.example.moliyaapp.repository.StudentContractRepository;
import org.example.moliyaapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// Real implementation when Google Sheets is enabled
@Service
@ConditionalOnProperty(
        name = "google.sheets.enabled",
        havingValue = "true"
)
@Primary  // Add this to prioritize this implementation when enabled
@Slf4j
@RequiredArgsConstructor
public class GoogleSheetsServiceImpl implements GoogleSheetsOperations {

    private final Sheets sheetsService;
    private final StudentContractRepository studentContractRepository;
    private final UserRepository userRepository;

    private static final String USER_ENTERED = "USER_ENTERED";
    private static final String INSERT_ROWS = "INSERT_ROWS";

    @Value("${google.sheets.student.contracts.sheet.id}")
    private String studentContractsSheetId;

    @Value("${google.sheets.student.contracts.range}")
    private String studentContractsRange;

    @Override
    public void recordStudentContract(StudentContract contract, StudentTariff tariff, Company company) {
        Long createdBy = contract.getCreatedBy();
        Long updatedBy = contract.getUpdatedBy();

        User create = this.userRepository.findByIdAndDeletedFalse(createdBy)
                .orElse(null);

        User update = this.userRepository.findByIdAndDeletedFalse(updatedBy)
                .orElse(null);

        try {
            List<Object> row = Arrays.asList(
                    contract.getUniqueId() != null ? contract.getUniqueId() : "",
                    contract.getContractedDate() != null ? contract.getContractedDate().toString() : "",
                    contract.getStudentFullName() != null ? contract.getStudentFullName() : "",
                    contract.getBirthDate() != null ? contract.getBirthDate().toString() : "",
                    contract.getGender() != null ? contract.getGender().toString() : "",
                    contract.getGrade() != null ? contract.getGrade().toString() : "",
                    contract.getStGrade() != null ? contract.getStGrade().toString() : "",
                    contract.getLanguage() != null ? contract.getLanguage().toString() : "",
                    contract.getGuardianFullName() != null ? contract.getGuardianFullName() : "",
                    contract.getGuardianType() != null ? contract.getGuardianType().toString() : "",
                    contract.getGuardianJSHSHIR() != null ? contract.getGuardianJSHSHIR() : "",
                    contract.getPassportId() != null ? contract.getPassportId() : "",
                    contract.getPassportIssuedBy() != null ? contract.getPassportIssuedBy() : "",
                    contract.getAcademicYear() != null ? contract.getAcademicYear() : "",
                    contract.getPhone1() != null ? contract.getPhone1() : "",
                    contract.getPhone2() != null ? contract.getPhone2() : "",
                    contract.getAddress() != null ? contract.getAddress() : "",
                    contract.getComment() != null ? contract.getComment() : "",
                    contract.getAmount() != null ? contract.getAmount().toString() : "",
                    contract.getContractStartDate() != null ? contract.getContractStartDate().toString() : "",
                    contract.getContractEndDate() != null ? contract.getContractEndDate().toString() : "",
                    contract.getClientType() != null ? contract.getClientType().toString() : "",
                    contract.getInactiveDate() != null ? contract.getInactiveDate().toString() : "",
                    contract.getBCN() != null ? contract.getBCN() : "",
                    company.getName() != null ? company.getName() : "",
                    tariff.getName() != null ? tariff.getName() : "",
                    contract.getCreatedAt() != null ? contract.getCreatedAt().toString() : "",
                    contract.getUpdatedAt() != null ? contract.getUpdatedAt().toString() : "",
                    contract.getStatus() != null && contract.getStatus() ? "Active" : "Passive",
                    contract.getDeleted() != null && contract.getDeleted() ? "Ha" : "Yoq",
                    create != null ? create.getFullName() : "N/A",
                    update != null ? update.getFullName() : "N/A"
            );

            ValueRange appendBody = new ValueRange()
                    .setValues(Collections.singletonList(row));

            // Use append instead of update for adding new rows
            AppendValuesResponse appendResult = this.sheetsService.spreadsheets().values()
                    .append(studentContractsSheetId, studentContractsRange, appendBody)
                    .setValueInputOption(USER_ENTERED)
                    .setInsertDataOption(INSERT_ROWS)
                    .execute();

            log.info("Student contract recorded in Google Sheets. {} cells appended.",
                    appendResult.getUpdates().getUpdatedCells());

        } catch (IOException e) {
            log.error("Error recording student contract to Google Sheets", e);
        }
    }

    @Override
    public void initializeSheet() {
        try {
            ValueRange headerRange = sheetsService.spreadsheets().values()
                    .get(studentContractsSheetId, studentContractsRange)
                    .execute();

            if (headerRange.getValues() == null || headerRange.getValues().isEmpty()) {
                List<Object> headers = Arrays.asList(
                        "O'quvchi ID", "shartnoma tuzulgan kun", "O'quvchi F.I.SH", "Tug'ilgan kun",
                        "Jins", "Sinf", "Sinf (harf)", "Til", "Vasiy F.I.SH",
                        "Vasiy turi", "Vasiy JSHSHIR", "Vasiy passport ID", "Amal qilish muddati",
                        "O'quv yili", "telefon 1", "telefon 2", "Manzil", "izoh",
                        "To'lov miqdori", "Shartnoma boshlanish sanasi", "Shartnoma tugash sanasi", "Klent turi",
                        "passiv sana", "Tug'ilganlik haqida guvohnoma", "Tashkilot", "Tarif",
                        "Qo'shilgan vaqt", "Status Text", "O'chirilganmi!?", "Tahrirlangan vaqt", "Kim qo'shdi!?", "Kim tahrirladi!?"
                );

                ValueRange headerBody = new ValueRange()
                        .setValues(Collections.singletonList(headers));

                sheetsService.spreadsheets().values()
                        .update(studentContractsSheetId, studentContractsRange, headerBody)
                        .setValueInputOption("USER_ENTERED")
                        .execute();

                this.bulkExportAllContracts();

                log.info("Headers initialized in Google Sheets with {} columns", headers.size());
            }
        } catch (IOException e) {
            log.error("Error initializing Google Sheets headers", e);
        }
    }

    private void bulkExportAllContracts() throws IOException {
        List<StudentContract> list = studentContractRepository.findAll();
        if (list.isEmpty()) {
            log.info("No contracts in DB to export.");
            return;
        }

        List<List<Object>> rows = new ArrayList<>();
        for (StudentContract c : list) {
            rows.add(toRow(c));
        }

        ValueRange body = new ValueRange().setValues(rows);
        sheetsService.spreadsheets().values()
                .append(studentContractsSheetId, studentContractsRange, body)
                .setValueInputOption(USER_ENTERED)
                .setInsertDataOption(INSERT_ROWS)
                .execute();

        log.info("{} contracts exported to Google Sheets (bulk).", rows.size());
    }


    private List<Object> toRow(StudentContract contract) {
        Long createdBy = contract.getCreatedBy();
        Long updatedBy = contract.getUpdatedBy();

        User create = this.userRepository.findByIdAndDeletedFalse(createdBy)
                .orElse(null);

        User update = this.userRepository.findByIdAndDeletedFalse(updatedBy)
                .orElse(null);

        List<Object> row = new ArrayList<>();
        row.add(contract.getUniqueId() != null ? contract.getUniqueId() : "");
        row.add(contract.getContractedDate() != null ? contract.getContractedDate().toString() : "");
        row.add(contract.getStudentFullName() != null ? contract.getStudentFullName() : "");
        row.add(contract.getBirthDate() != null ? contract.getBirthDate().toString() : "");
        row.add(contract.getGender() != null ? contract.getGender().toString() : "");
        row.add(contract.getGrade() != null ? contract.getGrade().toString() : "");
        row.add(contract.getStGrade() != null ? contract.getStGrade().toString() : "");
        row.add(contract.getLanguage() != null ? contract.getLanguage().toString() : "");
        row.add(contract.getGuardianFullName() != null ? contract.getGuardianFullName() : "");
        row.add(contract.getGuardianType() != null ? contract.getGuardianType().toString() : "");
        row.add(contract.getGuardianJSHSHIR() != null ? contract.getGuardianJSHSHIR() : "");
        row.add(contract.getPassportId() != null ? contract.getPassportId() : "");
        row.add(contract.getPassportIssuedBy() != null ? contract.getPassportIssuedBy() : "");
        row.add(contract.getAcademicYear() != null ? contract.getAcademicYear() : "");
        row.add(contract.getPhone1() != null ? contract.getPhone1() : "");
        row.add(contract.getPhone2() != null ? contract.getPhone2() : "");
        row.add(contract.getAddress() != null ? contract.getAddress() : "");
        row.add(contract.getComment() != null ? contract.getComment() : "");
        row.add(contract.getAmount() != null ? contract.getAmount().toString() : "");
        row.add(contract.getContractStartDate() != null ? contract.getContractStartDate().toString() : "");
        row.add(contract.getContractEndDate() != null ? contract.getContractEndDate().toString() : "");
        row.add(contract.getClientType() != null ? contract.getClientType().toString() : "");
        row.add(contract.getInactiveDate() != null ? contract.getInactiveDate().toString() : "");
        row.add(contract.getBCN() != null ? contract.getBCN() : "");
        row.add(contract.getCompany() != null ? contract.getCompany().getName() : "");
        row.add(contract.getTariff() != null ? contract.getTariff().getName() : "");
        row.add(contract.getCreatedAt() != null ? contract.getCreatedAt().toString() : "");
        row.add(contract.getUpdatedAt() != null ? contract.getUpdatedAt().toString() : "");
        row.add(contract.getStatus() != null && contract.getStatus() ? "Active" : "Passive");
        row.add(contract.getDeleted() != null && contract.getDeleted() ? "Ha" : "Yoq");
        row.add(create != null ? create.getFullName() : null);
        row.add(update != null ? update.getFullName() : null);
        return row;
    }

    public void updateStudentContractRow(StudentContract contract) {
        Long createdBy = contract.getCreatedBy();
        Long updatedBy = contract.getUpdatedBy();

        User create = this.userRepository.findByIdAndDeletedFalse(createdBy)
                .orElse(null);

        User update = this.userRepository.findByIdAndDeletedFalse(updatedBy)
                .orElse(null);

        try {
            // 1️⃣ Read all IDs from column A
            ValueRange response = sheetsService.spreadsheets().values()
                    .get(studentContractsSheetId, studentContractsRange) // skip header
                    .execute();

            List<List<Object>> values = response.getValues();
            if (values == null || values.isEmpty()) return;

            int rowIndex = -1;
            for (int i = 0; i < values.size(); i++) {
                String cellId = values.get(i).get(0).toString().trim();
                if (!cellId.isEmpty() && cellId.equals(String.valueOf(contract.getUniqueId()))) {
                    rowIndex = i + 1; // +1 for 1-based index, +1 to skip header
                    break;
                }
            }

            // 2️⃣ If not found → append new row
            if (rowIndex == -1) {
                recordStudentContract(contract, contract.getTariff(), contract.getCompany());
                return;
            }

            // 3️⃣ Prepare updated row
            List<Object> row = Arrays.asList(
                    contract.getUniqueId() != null ? contract.getUniqueId() : "",
                    contract.getContractedDate() != null ? contract.getContractedDate().toString() : "",
                    contract.getStudentFullName() != null ? contract.getStudentFullName() : "",
                    contract.getBirthDate() != null ? contract.getBirthDate().toString() : "",
                    contract.getGender() != null ? contract.getGender().toString() : "",
                    contract.getGrade() != null ? contract.getGrade().toString() : "",
                    contract.getStGrade() != null ? contract.getStGrade().toString() : "",
                    contract.getLanguage() != null ? contract.getLanguage().toString() : "",
                    contract.getGuardianFullName() != null ? contract.getGuardianFullName() : "",
                    contract.getGuardianType() != null ? contract.getGuardianType().toString() : "",
                    contract.getGuardianJSHSHIR() != null ? contract.getGuardianJSHSHIR() : "",
                    contract.getPassportId() != null ? contract.getPassportId() : "",
                    contract.getPassportIssuedBy() != null ? contract.getPassportIssuedBy() : "",
                    contract.getAcademicYear() != null ? contract.getAcademicYear() : "",
                    contract.getPhone1() != null ? contract.getPhone1() : "",
                    contract.getPhone2() != null ? contract.getPhone2() : "",
                    contract.getAddress() != null ? contract.getAddress() : "",
                    contract.getComment() != null ? contract.getComment() : "",
                    contract.getAmount() != null ? contract.getAmount().toString() : "",
                    contract.getContractStartDate() != null ? contract.getContractStartDate().toString() : "",
                    contract.getContractEndDate() != null ? contract.getContractEndDate().toString() : "",
                    contract.getClientType() != null ? contract.getClientType().toString() : "",
                    contract.getInactiveDate() != null ? contract.getInactiveDate().toString() : "",
                    contract.getBCN() != null ? contract.getBCN() : "",
                    contract.getCompany() != null ? contract.getCompany().getName() : "",
                    contract.getTariff() != null ? contract.getTariff().getName() : "",
                    contract.getCreatedAt() != null ? contract.getCreatedAt().toString() : "",
                    contract.getStatus() != null && contract.getStatus() ? "Active" : "Passive",
                    contract.getDeleted() != null && contract.getDeleted() ? "Ha" : "Yoq",
                    contract.getUpdatedAt() != null ? contract.getUpdatedAt().toString() : "",
                    create != null ? create.getFullName() : "",
                    update != null ? update.getFullName() : ""
            );

            ValueRange body = new ValueRange().setValues(Collections.singletonList(row));

            String range = "Shartnoma(STD)!A" + rowIndex + ":AF" + rowIndex; // AC = last column
            sheetsService.spreadsheets().values()
                    .update(studentContractsSheetId, range, body)
                    .setValueInputOption(USER_ENTERED)
                    .execute();

        } catch (Exception e) {
            log.error("Failed to update student contract in Google Sheet", e);
        }
    }


}
