package org.example.moliyaapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity(name = "roles")
public class UserRole extends BaseEntity {

    @Column(unique = true, nullable = false)
    private String name;
    
}
