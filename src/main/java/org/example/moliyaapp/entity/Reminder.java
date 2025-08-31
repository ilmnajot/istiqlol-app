package org.example.moliyaapp.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.moliyaapp.enums.Months;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
@Table(name = "reminder")
public class Reminder extends BaseEntity {

    @ManyToOne
    private StudentContract studentContract;

    @Enumerated(EnumType.STRING)
    private Months month;

    private Boolean isReminded;

    private LocalDateTime estimatedTime;

    private String comment;

}
