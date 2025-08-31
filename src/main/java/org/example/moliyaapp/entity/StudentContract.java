package org.example.moliyaapp.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.example.moliyaapp.enums.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "student_contracts")
@Builder
public class StudentContract extends BaseEntity {

    private LocalDate contractedDate; // Sana
    private String studentFullName; // O'quvchi F.I.SH.
    private LocalDate birthDate; // Tug‘ilgan sana
    @Enumerated(EnumType.STRING)
    private Gender gender; // Jinsi
    @Enumerated(EnumType.STRING)
    private StudentGrade grade; // Sinf
    @Enumerated(EnumType.STRING)
    private Grade stGrade;
    @Enumerated(EnumType.STRING)
    private StudyLanguage language; // Tili

    private String guardianFullName; // Vasiy F.I.SH.
    @Enumerated(EnumType.STRING)
    private GuardianType guardianType; // Vasiy turi
    private String guardianJSHSHIR; // Vasiy JSHSHIR unik
    private String passportId; // Pasport ID
    private String passportIssuedBy; // Berilgan joyi

    private String academicYear; // O'quv yili

    private String phone1; // Tel 1
    private String phone2; // Tel 2

    private String address; // Manzil
    private String comment; // Izoh

    private Double amount; //oylik yoki yillik miqdor

    private LocalDate contractStartDate; // Shartnoma boshlanish sanasi
    private LocalDate contractEndDate; // Shartnoma tugash sanasi
    @Enumerated(EnumType.STRING)
    private ClientType clientType;
    private Boolean status; // Xolati (ACTIVE)
    private LocalDate inactiveDate = null; // shartnomani bekro qilish sanasi
    private String BCN;
    private String uniqueId; // shartnoma raqami


    //******************filter uchun qo'shildi******************//
    @OneToMany(mappedBy = "studentContract", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MonthlyFee> monthlyFees = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;

    @ManyToOne
    @JoinColumn(name = "student_tariff_id")
    private StudentTariff tariff;

}
