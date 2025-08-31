package org.example.moliyaapp.enums;

public enum TariffStatus {
    YEARLY("Yillik"),
    MONTHLY("Oylik"),
    QUARTERLY("Choraklik");

    private final String value;

    TariffStatus(String value) {
        this.value = value;
    }
}
