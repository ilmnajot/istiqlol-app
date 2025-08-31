package org.example.moliyaapp.controller;

import org.example.moliyaapp.dto.ApiResponse;
import org.example.moliyaapp.service.EnumService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/enums")
public class EnumController {

    private final EnumService enumService;

    public EnumController(EnumService enumService) {
        this.enumService = enumService;
    }

    @GetMapping("/get-months")
    public ApiResponse getMonths() {
        return this.enumService.getMonths();
    }

    @GetMapping("/get-payment-status")
    public ApiResponse getPaymentStatus() {
        return this.enumService.getPaymentStatus();
    }

    @GetMapping("/get-payment-type")
    public ApiResponse getPaymentTypes() {
        return this.enumService.getPaymentTypes();
    }

    @GetMapping("/get-transaction-type")
    public ApiResponse getTransactionTypes() {
        return this.enumService.getTransactionType();
    }

    @GetMapping("/get-weeks")
    public ApiResponse getWeeks() {
        return this.enumService.getWeeks();
    }

    @GetMapping("/get-user-status")
    public ApiResponse getUserStatus() {
        return this.enumService.getUserStatus();
    }
    @GetMapping("/get-all-role-names")
    public ApiResponse getAllRoleNames(){
        return this.enumService.getAllRoleNames();
    }

}
