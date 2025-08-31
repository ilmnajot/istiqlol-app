package org.example.moliyaapp.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "company")
@Builder
public class Company extends BaseEntity {

    private String name;
    private Double cashBalance;  // Naqd pul balansi
    private Double cardBalance; // Karta balansi
    @OneToOne
    private User user;

}
