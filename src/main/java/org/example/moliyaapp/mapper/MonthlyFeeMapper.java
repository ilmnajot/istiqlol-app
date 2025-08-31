package org.example.moliyaapp.mapper;

import lombok.RequiredArgsConstructor;
import org.example.moliyaapp.dto.MonthlyFeeDto;
import org.example.moliyaapp.entity.MonthlyFee;
import org.example.moliyaapp.entity.Transaction;
import org.example.moliyaapp.repository.TransactionRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Component
public class MonthlyFeeMapper {

    private final TransactionRepository transactionRepository;


    public MonthlyFee toEntity(MonthlyFeeDto.CreateMonthlyFee dto) {
        return MonthlyFee.builder()
                .months(dto.getMonths())
                .amountPaid(dto.getAmountPaid())
                .build();
    }

    public MonthlyFeeDto toDto(MonthlyFee monthlyFee) {

        return MonthlyFeeDto.builder()
                .id(monthlyFee.getId())
                .months(monthlyFee.getMonths())
                .totalFee(monthlyFee.getTotalFee() )
                .amountPaid(monthlyFee.getAmountPaid())
                .remainingBalance(monthlyFee.getRemainingBalance())
                .bonus(monthlyFee.getBonus() != null ? monthlyFee.getBonus() : 0.0)
                .penalty(monthlyFee.getPenalty() != null ? monthlyFee.getPenalty() : 0.0)
                .discount(monthlyFee.getDiscount() != null ? monthlyFee.getDiscount() : 0.0)
                .employeeId(monthlyFee.getEmployee() != null && monthlyFee.getEmployee().getId() != null ? monthlyFee.getEmployee().getId() : 0)
                .employeeName(monthlyFee.getEmployee() != null && monthlyFee.getEmployee().getFullName() != null ? monthlyFee.getEmployee().getFullName() : "")
                .studentContractId(monthlyFee.getStudentContract() != null && monthlyFee.getStudentContract().getId() != null ? monthlyFee.getStudentContract().getId() : 0)
                .studentName(monthlyFee.getStudentContract() != null ? monthlyFee.getStudentContract().getStudentFullName() : "")
                .paymentStatus(monthlyFee.getStatus())
                .categoryName(monthlyFee.getCategory() != null ? monthlyFee.getCategory().getName():null)
                .createdAt(monthlyFee.getCreatedAt())
                .createdBy(monthlyFee.getCreatedBy())
                .updatedAt(monthlyFee.getUpdatedAt())
                .updatedBy(monthlyFee.getUpdatedBy())
                .deleted(monthlyFee.getDeleted())
                .build();
    }


    public List<MonthlyFeeDto> dtoList(List<MonthlyFee> list) {
        if (list != null && !list.isEmpty()) {
            return list.stream().map(this::toDto).toList();
        }
        return new ArrayList<>();
    }


}
