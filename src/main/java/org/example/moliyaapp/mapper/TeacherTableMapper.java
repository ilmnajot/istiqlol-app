package org.example.moliyaapp.mapper;

import lombok.RequiredArgsConstructor;
import org.example.moliyaapp.dto.ExtraLessonPriceDto;
import org.example.moliyaapp.dto.TeacherTableDto;
import org.example.moliyaapp.dto.UserDto;
import org.example.moliyaapp.entity.*;
import org.example.moliyaapp.enums.Months;
import org.example.moliyaapp.enums.PaymentType;
import org.example.moliyaapp.enums.SalaryType;
import org.example.moliyaapp.repository.MonthlyFeeRepository;
import org.example.moliyaapp.repository.TeacherContractRepository;
import org.example.moliyaapp.repository.TransactionRepository;
import org.example.moliyaapp.service.impl.TeacherTableServiceImpl;
import org.example.moliyaapp.utils.RestConstants;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class TeacherTableMapper {

    private final UserMapper userMapper;
    private final ExtraLessonPriceMapper extraLessonPriceMapper;
    private final MonthlyFeeRepository monthlyFeeRepository;
    private final TransactionRepository transactionRepository;


    public TeacherTableDto toDto(TeacherTable table) {
        User user = table.getTeacher();
        UserDto userDto = this.userMapper.toDto(user);

        ExtraLessonPrice extraLessonPrice = table.getExtraLessonPrice();
        ExtraLessonPriceDto lessonPriceDto = null;

        if (extraLessonPrice != null) {
            lessonPriceDto = this.extraLessonPriceMapper.toDto(extraLessonPrice);
        }

        // hisoblashlar
        int daysOrLessonPerMonth = table.getWorkDaysOrLessons() != null
                ? table.getWorkDaysOrLessons()
                : 0;
        int extra = table.getExtraWorkedDaysOrLessons() != null
                ? table.getExtraWorkedDaysOrLessons()
                : 0;
        int missed = table.getMissedWorkDaysOrLessons() != null
                ? table.getMissedWorkDaysOrLessons()
                : 0;

        double salary = table.getMonthlySalary() != null
                ? table.getMonthlySalary()
                : 0;
        SalaryType salaryType = table.getTeacherContract().getSalaryType();

        double extraAmount = 0.0;
        double cuttingAmount;
        double fixedAmount = extraLessonPrice != null ? extraLessonPrice.getFixedAmount() : 0;

        if (salaryType == SalaryType.PRICE_PER_LESSON) {
            extraAmount = extra * fixedAmount;
            cuttingAmount = missed * salary;

        } else if (salaryType == SalaryType.PRICE_PER_MONTH) {
            extraAmount = extra * fixedAmount;
            cuttingAmount = TeacherTableServiceImpl.roundTo1000(missed * salary / daysOrLessonPerMonth);

        } else { // PRICE_PER_DAY
            cuttingAmount = TeacherTableServiceImpl.roundTo1000(missed * salary);
        }
        MonthlyFee fee = monthlyFeeRepository.findByEmployeeIdAndMonths(user.getId(), table.getMonths())
                .orElse(null);

        double cardAmount = 0.0;
        double cashAmount = 0.0;
        double leftAmount = 0.0;
        double bonusAmount = 0.0;

        if (fee == null) {
            leftAmount = table.getAmount();
        }

        if (fee != null) {
            leftAmount = fee.getRemainingBalance() != null ? fee.getRemainingBalance() : 0.0;
            bonusAmount = fee.getBonus() != null ? fee.getBonus() : 0.0;
            Long feeId = fee.getId();

            List<Transaction> transactionList = this.transactionRepository.findAllByMonthlyFeeId(feeId);
            for (Transaction transaction : transactionList) {
                if (transaction.getPaymentType() == PaymentType.BANK) {
                    cardAmount += transaction.getAmount();
                } else if (transaction.getPaymentType() == PaymentType.NAQD) {
                    cashAmount += transaction.getAmount();
                }
            }
        }

        return TeacherTableDto.builder()
                .id(table.getId())

                .months(table.getMonths())
                .userDto(userDto)
                .teacherContract(table.getTeacherContract().getId())
                .finalAmount(table.getAmount())
                .description(table.getDescription())
                .extraLessonPriceDto(lessonPriceDto)

                .workDaysOrLessons(table.getWorkDaysOrLessons())
                .workedDaysOrLessons(table.getWorkedDaysOrLessons())
                .extraWorkedDaysOrLessons(table.getExtraWorkedDaysOrLessons())
                .missedWorkDaysOrLessons(table.getMissedWorkDaysOrLessons())

//                .cashAmount(cashAmount - bonusAmount)
                .cashAmount(cashAmount)
                .cardAmount(cardAmount)
                .remainingBalance(leftAmount)
                .bonusAmount(bonusAmount)

                .salary(table.getMonthlySalary())

                .extraAmount(extraAmount)
                .cuttingAmount(cuttingAmount)

                .createdAt(table.getCreatedAt())
                .updatedAt(table.getUpdatedAt())
                .createdBy(table.getCreatedBy())
                .updatedBy(table.getUpdatedBy())
                .deleted(table.getDeleted())
                .build();
    }


    public TeacherTable toEntity(TeacherTableDto.CreateTeacherTableDto dto) {
        return TeacherTable.builder()
                .workDaysOrLessons(dto.getWorkDaysOrLessons())
                .workedDaysOrLessons(dto.getWorkedDaysOrLessons())
                .missedWorkDaysOrLessons(dto.getWorkDaysOrLessons() - dto.getWorkedDaysOrLessons())
                .extraWorkedDaysOrLessons(dto.getExtraWorkedDaysOrLessons())
                .months(dto.getMonths())
                .description(dto.getDescription())
                .build();
    }

    public void toUpdate(TeacherTable table, TeacherTableDto.CreateTeacherTableDto dto) {

        if (dto.getMonths() != null) {
            table.setMonths(dto.getMonths());
        }
        if (dto.getDescription() != null && !dto.getDescription().trim().isEmpty()) {
            table.setDescription(dto.getDescription());
        }

    }

    public List<TeacherTableDto> toDto(List<TeacherTable> dtoList) {
        if (dtoList != null && !dtoList.isEmpty()) {
            return dtoList
                    .stream()
                    .map(this::toDto)
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
