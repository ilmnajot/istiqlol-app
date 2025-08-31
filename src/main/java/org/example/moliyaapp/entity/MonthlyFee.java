package org.example.moliyaapp.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.moliyaapp.enums.Months;
import org.example.moliyaapp.enums.PaymentStatus;
import org.example.moliyaapp.enums.TariffName;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "monthly_fee")
public class MonthlyFee extends BaseEntity {

    @Enumerated(EnumType.STRING)
    private Months months;

    @Column(nullable = false)
    private Double totalFee = 0.0;  // Total required amount (e.g., 300 USD per month)

    @Column(nullable = false)
    private Double amountPaid = 0.0;  // Total amount student has paid so far

    private Double remainingBalance = 0.0;  // Amount left to pay (totalFee - amountPaid)

    private Double discount = 0.0;  // Any discounts applied

    private Double bonus = 0.0; //premiya, va boshqa

    private Double penalty = 0.0; // shtraf

    private Double cutAmount = 0.0; // new field // chegirish uchun!

    private String reason;

    @Builder.Default
    private Boolean isAdvanced = Boolean.FALSE;

    @Column(name = "tariff_name", length = 50)
    private String tariffName;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;  // FULLY_PAID, PARTIALLY_PAID, UNPAID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_contract_id")
    private StudentContract studentContract;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private User employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @OneToMany(mappedBy = "monthlyFee", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Image> images = new ArrayList<>();



}
