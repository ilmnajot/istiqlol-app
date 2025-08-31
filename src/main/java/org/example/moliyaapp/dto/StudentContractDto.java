package org.example.moliyaapp.dto;

import lombok.*;
import org.example.moliyaapp.enums.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StudentContractDto {

    private Long id;
    private LocalDate contractedDate; // Sana
    private String studentFullName; // O'quvchi F.I.SH.
    private LocalDate birthDate; // Tug‘ilgan sana
    private Gender gender; // Jinsi
    private StudentGrade grade; // Sinf
    private Grade stGrade;
    private StudyLanguage language; // Tili
    private Long companyId;
    private String guardianFullName; // Vasiy F.I.SH.
    private GuardianType guardianType; // Vasiy turi
    private String guardianJSHSHIR; // Vasiy JSHSHIR unik
    private String passportId; // Pasport ID
    private String passportIssuedBy; // Berilgan joyi
    private String academicYear; // O'quv yili
    private String phone1; // Tel 1
    private String phone2; // Tel 2
    private String address; // Manzil
    private String comment; // Izoh
    private String BCN;
    private Double amount;
    private StudentTariffDto tariffDto;
    private LocalDate contractStartDate; // Shartnoma boshlanish sanasi
    private LocalDate contractEndDate; // Shartnoma tugash sanasi
    private ClientType clientType; // Mijoz turi (YANGI)
    private Boolean status; // Xolati (ACTIVE)
    private LocalDate inactiveDate; // shartnomani bekor qilish sanasi
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean deleted;
    private List<MonthlyFeeDto> payments; // To'lovlar ro'yxati
    private List<ReminderDto> reminderDtos;
    private Double remain;
    private String uniqueId; // shartnoma raqami


    @Setter
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class CreateStudentContractDto {

//        private String contractNo;
        private LocalDate contractedDate; // Sana
        private String studentFullName; // O'quvchi F.I.SH.
        private LocalDate birthDate; // Tug‘ilgan sana
        private Gender gender; // Jinsi
        private StudentGrade grade; // Sinf
        private Grade stGrade;
        private StudyLanguage language; // Tili
        private String guardianFullName; // Vasiy F.I.SH.
        private GuardianType guardianType; // Vasiy turi
        private String guardianJSHSHIR; // Vasiy JSHSHIR unik
        private String passportId; // Pasport ID
        private String passportIssuedBy; // Berilgan joyi
        private String academicYear; // O'quv yili
        private String phone1; // Tel 1
        private String phone2; // Tel 2
        private String address; // Manzil
        private String comment; // Izoh
        private String BCN;
        private LocalDate contractStartDate; // Shartnoma boshlanish sanasi
        private LocalDate contractEndDate; // Shartnoma tugash sanasi
        private ClientType clientType; // Mijoz turi (YANGI)
        private Boolean status; // Xolati (ACTIVE)
//        private LocalDate inactiveDate; // shartnomani bekro qilish sanasi
        private Long tariffId;
//        private String uniqueId; // shartnoma raqami
    }


    @Setter
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class UpdateStudentContractDto {

//        private String contractNo;
        private LocalDate contractedDate; // Sana
        private String studentFullName; // O'quvchi F.I.SH.
        private LocalDate birthDate; // Tug‘ilgan sana
        private Gender gender; // Jinsi
        private StudentGrade grade; // Sinf
        private Grade stGrade;
        private StudyLanguage language; // Tili
        private String guardianFullName; // Vasiy F.I.SH.
        private GuardianType guardianType; // Vasiy turi
        private String guardianJSHSHIR; // Vasiy JSHSHIR unik
        private String passportId; // Pasport ID
        private String passportIssuedBy; // Berilgan joyi
        private String academicYear; // O'quv yili
        private String phone1; // Tel 1
        private String phone2; // Tel 2
        private String address; // Manzil
        private String comment; // Izoh
        private String BCN;
        private Double amount;

        // To'lov oylarini belgilash uchun
//        private Map<Months, Double> payments;
        private LocalDate contractStartDate; // Shartnoma boshlanish sanasi
        private LocalDate contractEndDate; // Shartnoma tugash sanasi
        private ClientType clientType; // Mijoz turi (YANGI)
        private Boolean status; // Xolati (ACTIVE)
        private LocalDate inactiveDate; // shartnomani bekro qilish sanasi
        private Long tariffId;
    }
    @Data
    public static class StudentPaymentDto{
        private Double amountPaid;
        private Months month;
        private PaymentType paymentType;
        private String comment;
    }
    @Data
    public static class EmployeePaymentDto{
        private Double newAmount;
        private String comment;
    }  @Data
    public static class EmployeeBonusPaymentDto{
        private Double bonus;
        private String comment;
    }


}
