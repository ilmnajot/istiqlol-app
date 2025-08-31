package org.example.moliyaapp.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.*;
import org.example.moliyaapp.enums.TariffStatus;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "student_tariff")
@Builder
public class StudentTariff extends BaseEntity {

    @NotBlank(message = "Tariff nomi majburiy!")
    private String name;

    @Positive
    private Double amount;

    @Enumerated(EnumType.STRING)
    private TariffStatus tariffStatus;

}
