package org.example.moliyaapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;
import jakarta.persistence.Entity;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
@Table(name = "sms")
public class Sms extends BaseEntity {

    private String phoneNumber;
    private String title;
    @Column(columnDefinition = "TEXT", nullable = false)
    private String text;

    @ManyToOne
    private StudentContract studentContract;

    @ManyToOne
    private MonthlyFee monthlyFee;
}
