package org.example.moliyaapp.service;

import org.example.moliyaapp.dto.ApiResponse;

public interface EnumService {
    ApiResponse getMonths();

    ApiResponse getPaymentStatus();

    ApiResponse getPaymentTypes();

    ApiResponse getTransactionType();

    ApiResponse getWeeks();

    ApiResponse getUserStatus();

    ApiResponse getAllRoleNames();
}
