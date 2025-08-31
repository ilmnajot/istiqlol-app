package org.example.moliyaapp.projection;

public interface GetAmountByQuarter {
    String getQuarter();
    Double getTotalAmount();
    Double getPaidAmount();
    Double getUnPaidAmount();
    Double getCuttingAmount();
}