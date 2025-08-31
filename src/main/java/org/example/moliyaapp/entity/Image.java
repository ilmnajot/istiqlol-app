package org.example.moliyaapp.entity;

import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;
import jakarta.persistence.Entity;

import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
@Table(name = "images")
public class Image extends BaseEntity {

    private String url;

    @ManyToOne
    @JoinColumn(name = "monthly_fee_id")
    private MonthlyFee monthlyFee;

    @ManyToOne
    @JoinColumn(name = "expense_id")
    private Expenses expenses;
}
