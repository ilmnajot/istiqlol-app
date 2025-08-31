package org.example.moliyaapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
@Table(name = "extra_lesson_price")
public class ExtraLessonPrice extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Double fixedAmount;

}
