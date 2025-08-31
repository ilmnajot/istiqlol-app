package org.example.moliyaapp.enums;

import lombok.Getter;

@Getter
public enum
TransactionType {
    INCOME("KIRIM"),
    OUTCOME("CHIQIM");
    private final String value;

    TransactionType(String value) {
        this.value = value;
    }
}
