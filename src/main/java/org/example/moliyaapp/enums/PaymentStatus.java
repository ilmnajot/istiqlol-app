package org.example.moliyaapp.enums;

import lombok.Getter;

@Getter
public enum PaymentStatus {
    FULLY_PAID("To'liq to'langan"),
    PARTIALLY_PAID("Qisman to'langan"),
    UNPAID("To'lanmagan");

    private final String name;

    PaymentStatus(String name) {
        this.name = name;
    }
}