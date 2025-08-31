package org.example.moliyaapp.dto;

import lombok.*;
import jakarta.persistence.Entity;
import org.example.moliyaapp.enums.TariffStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TariffChangeHistoryDto {
    private Long id;
    private Long tariffId;
    private Long studentContractId;
    private TariffStatus tariffStatus; // TariffStatus enum nomi sifatida saqlanadi
    private Double tariffAmount;
    private LocalDate fromDate; // YYYY-MM-DD formatida saqlanadi
    private LocalDate toDate; // YYYY-MM-DD formatida saqlanadi, null bo‘lsa, hozirgacha amal qiladi
    private String tariffName;
    private String reason; // Tarif o‘zgarishi sababi


    private LocalDateTime createdAt; // YYYY-MM-DD HH:mm:ss formatida saqlanadi
    private LocalDateTime updatedAt; // YYYY-MM-DD HH:mm:ss formatida saqlanadi
    private Long createdBy; // User ID
    private Long updatedBy; // User ID
    private Boolean deleted;
}
