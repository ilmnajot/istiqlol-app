package org.example.moliyaapp.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.moliyaapp.enums.TariffStatus;

import java.time.LocalDate;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
public class TariffChangeHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_contract_id")
    private StudentContract studentContract;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_tariff_id")
    private StudentTariff tariff; // Add this field

    @Enumerated(EnumType.STRING)
    private TariffStatus tariffStatus;

    private Double tariffAmount;

    private LocalDate fromDate;

    private LocalDate toDate; // null bo‘lsa, hozirgacha amal qiladi

    private String reason; // Tarif o‘zgarishi sababi
}
