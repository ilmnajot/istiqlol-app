package org.example.moliyaapp.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.moliyaapp.entity.MonthlyFee;
import org.example.moliyaapp.entity.StudentContract;
import org.example.moliyaapp.repository.MonthlyFeeRepository;
import org.example.moliyaapp.repository.StudentContractRepository;
import org.example.moliyaapp.service.SmsService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentReminderService {

    private final MonthlyFeeRepository monthlyFeeRepository;
//    private final SmsService smsService;
    private final StudentContractRepository studentContractRepository;

//    @Scheduled(cron = "0 0 9 10 * ?") // Har oyning 10-sanasida 09:00 da ishga tushadi
//    @Scheduled(cron = "0 * * * * ?") // Har oyning 10-sanasida 09:00 da ishga tushadi
//    public void sendMonthlyPaymentReminders() {
//        int month = LocalDate.now().getMonthValue();
//        int year = LocalDate.now().getYear();
////        List<MonthlyFee> unpaidFees = monthlyFeeRepository.findUnpaidForMonth(month, year);
//
//        for (MonthlyFee fee : unpaidFees) {
//            StudentContract contract = fee.getStudentContract();
//
//            String studentName = contract.getStudentFullName();
//            String phoneNumber = contract.getPhone1(); // Ota-onaning raqami
//
//            String message = buildReminderText(studentName, BigDecimal.valueOf(fee.getRemainingBalance()));
//
//            smsService.sendSms(phoneNumber, message);
//
//            System.out.println("📤 SMS yuborildi: " + studentName + " - " + phoneNumber);
//        }
//    }

    private String buildReminderText(String studentName, BigDecimal amount) {
        return String.format("""
            Hurmatli ota-ona!
            %s uchun oylik to‘lov eslatmasi.
            Miqdor: %s so‘m.
            To‘lov muddati: 10-20 sana oralig‘ida.
            Iltimos, to‘lovni belgilangan muddatda amalga oshiring.
            """,
            studentName,
            amount.toPlainString()
        );
    }
}
