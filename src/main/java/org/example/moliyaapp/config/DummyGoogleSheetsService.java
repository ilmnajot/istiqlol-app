package org.example.moliyaapp.config;

import lombok.extern.slf4j.Slf4j;
import org.example.moliyaapp.entity.Company;
import org.example.moliyaapp.entity.StudentContract;
import org.example.moliyaapp.entity.StudentTariff;
import org.example.moliyaapp.repository.GoogleSheetsOperations;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

// Dummy implementation when Google Sheets is disabled
@Service
@ConditionalOnMissingBean(name = "googleSheetsServiceImpl")  // Only create if real one doesn't exist
@Slf4j
public class DummyGoogleSheetsService implements GoogleSheetsOperations {

    @Override
    public void recordStudentContract(StudentContract contract, StudentTariff tariff, Company company) {
        log.debug("Google Sheets integration is disabled. Contract recording skipped for BCN: {}",
                contract.getBCN());
    }

    @Override
    public void initializeSheet() {
        log.debug("Google Sheets integration is disabled. Sheet initialization skipped.");
    }

    @Override
    public void updateStudentContractRow(StudentContract contract) {

    }


}