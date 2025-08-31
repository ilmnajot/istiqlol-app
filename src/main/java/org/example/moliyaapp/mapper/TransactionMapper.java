package org.example.moliyaapp.mapper;

import org.example.moliyaapp.dto.TransactionDto;
import org.example.moliyaapp.entity.MonthlyFee;
import org.example.moliyaapp.entity.Transaction;
import org.example.moliyaapp.enums.Months;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TransactionMapper {

    public TransactionDto toDto(Transaction transaction) {
        TransactionDto dto = new TransactionDto();
        dto.setId(transaction.getId());
        dto.setAmount(transaction.getAmount());
        dto.setDescription(transaction.getDescription());
        dto.setTransactionType(transaction.getTransactionType());
        dto.setPaymentType(transaction.getPaymentType());
        dto.setCreatedAt(transaction.getCreatedAt());
        dto.setUpdatedAt(transaction.getUpdatedAt());
        dto.setCreatedBy(transaction.getCreatedBy());
        dto.setUpdatedBy(transaction.getUpdatedBy());
        dto.setDeleted(transaction.getDeleted());
        return dto;
    }

    public TransactionDto.ToFilterDto toFilterDto(Transaction transaction) {
        if (transaction == null) {
            return null;
        }
        MonthlyFee monthlyFee = transaction.getMonthlyFee();
        String studentFullName = null;
        String employeeName = null;
        Months month = null;
        String uniqueId = null;
        Long id=null;
        if (monthlyFee != null) {
            if (monthlyFee.getStudentContract() != null) {
                studentFullName = monthlyFee.getStudentContract().getStudentFullName();
            }
            if (monthlyFee.getStudentContract() != null) {
                id = monthlyFee.getStudentContract().getId();
            }
            if (monthlyFee.getEmployee() != null) {
                employeeName = monthlyFee.getEmployee().getFullName();
            }
            if (monthlyFee.getStudentContract()!=null){
                uniqueId = monthlyFee.getStudentContract().getUniqueId();
            }
            month = monthlyFee.getMonths();
        }
        // Determine amount type from description
        String amountType = determineAmountType(transaction.getDescription());

        TransactionDto.ToFilterDto dto = new TransactionDto.ToFilterDto();
        dto.setId(transaction.getId());
        dto.setAmount(transaction.getAmount());
        dto.setDescription(transaction.getDescription());
        dto.setTransactionType(transaction.getTransactionType());
        dto.setPaymentType(transaction.getPaymentType());
        dto.setStudentFullName(studentFullName);
        dto.setEmployeeFullName(employeeName);
        dto.setMonth(month);
        dto.setAcademicYear(
                monthlyFee != null && monthlyFee.getStudentContract() != null
                        ? monthlyFee.getStudentContract().getAcademicYear()
                        : null
        );
        dto.setStudentContractId(id);
        dto.setUniqueId(uniqueId);
        dto.setAmountType(amountType);
        dto.setCreatedAt(transaction.getCreatedAt());
        dto.setUpdatedAt(transaction.getUpdatedAt());
        dto.setCreatedBy(transaction.getCreatedBy());
        dto.setUpdatedBy(transaction.getUpdatedBy());
        dto.setDeleted(transaction.getDeleted());
        return dto;
    }

    // Helper method to determine amount type from description
    private String determineAmountType(String description) {
        if (description == null) {
            return null;
        }

        if (description.startsWith("ASOSIY_TO'LOV_TRANSAKSIYASI:")) {
            return "MAIN";
        } else if (description.startsWith("BONUS_TO'LOV_TRANSAKSIYASI:")) {
            return "BONUS";
        }

        return "OTHER"; // For other types of transactions
    }

    public List<TransactionDto> toDto(List<Transaction> transactionList) {
        if (transactionList != null && !transactionList.isEmpty()) {
            return transactionList.stream().map(this::toDto).toList();
        }
        return new ArrayList<>();
    }

    public List<TransactionDto.ToFilterDto> toFilterDto(List<Transaction> transactionList) {
        if (transactionList != null && !transactionList.isEmpty()) {
            return transactionList.stream().map(this::toFilterDto).toList();
        }
        return new ArrayList<>();
    }

}
