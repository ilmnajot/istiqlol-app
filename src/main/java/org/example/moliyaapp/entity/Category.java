package org.example.moliyaapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;
import org.example.moliyaapp.enums.CategoryStatus;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "category")
@Builder
public class Category extends BaseEntity {

    @Column(unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    private CategoryStatus categoryStatus;

}
