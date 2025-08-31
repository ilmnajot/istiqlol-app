package org.example.moliyaapp.dto;

import lombok.*;
import org.example.moliyaapp.enums.PaymentType;
import org.hibernate.type.descriptor.java.LocalDateJavaType;

import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PartnersDto {

    private Long id;
    private String name;
    private String bankAccount;
    private Long companyId;
    private String comment;
    private PaymentType paymentType;

    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean deleted;


    @Setter
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class CreateDto {

        private String name;
        private String bankAccount;
        private String comment;
        private PaymentType paymentType;
    }
}
