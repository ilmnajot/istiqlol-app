package org.example.moliyaapp.service.impl;

import org.example.moliyaapp.dto.ApiResponse;
import org.example.moliyaapp.entity.UserRole;
import org.example.moliyaapp.enums.*;
import org.example.moliyaapp.repository.RoleRepository;
import org.example.moliyaapp.service.EnumService;
import org.example.moliyaapp.utils.RestConstants;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class EnumServiceImpl implements EnumService {


    private final RoleRepository roleRepository;

    public EnumServiceImpl(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public ApiResponse getMonths() {
        List<String> list = Arrays
                .stream(Months.values())
                .map(Months::name)
                .toList();

        return ApiResponse.builder()
                .data(list)
                .message(RestConstants.SUCCESS)
                .status(HttpStatus.OK)
                .build();
    }

    @Override
    public ApiResponse getPaymentStatus() {
        List<String> list = Arrays
                .stream(PaymentStatus.values())
                .map(PaymentStatus::name)
                .toList();

        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESS)
                .data(list)
                .build();
    }

    @Override
    public ApiResponse getPaymentTypes() {
        List<String> list = Arrays
                .stream(PaymentType.values())
                .map(PaymentType::name)
                .toList();

        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESS)
                .data(list)
                .build();
    }

    @Override
    public ApiResponse getTransactionType() {
        List<String> list = Arrays
                .stream(TransactionType.values())
                .map(TransactionType::name)
                .toList();

        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESS)
                .data(list)
                .build();
    }

    @Override
    public ApiResponse getWeeks() {
        List<String> list = Arrays
                .stream(Weeks.values())
                .map(Weeks::name)
                .toList();

        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESS)
                .data(list)
                .build();
    }

    @Override
    public ApiResponse getUserStatus() {
        List<String> list = Arrays
                .stream(UserStatus.values())
                .map(UserStatus::name)
                .toList();

        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESS)
                .data(list)
                .build();
    }

    @Override
    public ApiResponse getAllRoleNames() {
        Set<String> collected = this.roleRepository.findAll()
                .stream().map(UserRole::getName)
                .collect(Collectors.toSet());

        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESS)
                .data(collected)
                .build();
    }
}
