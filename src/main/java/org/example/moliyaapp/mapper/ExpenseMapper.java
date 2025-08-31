package org.example.moliyaapp.mapper;

import org.example.moliyaapp.dto.ExpensesDto;
import org.example.moliyaapp.entity.Expenses;
import org.example.moliyaapp.entity.Image;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static org.example.moliyaapp.utils.Utils.getIfExists;

@Component
public class ExpenseMapper {
    public Expenses toEntity(ExpensesDto.CreateExpense dto) {
        return Expenses.builder()
                .amount(dto.getAmount())
                .description(dto.getDescription())
                .receiver(dto.getReceiver() != null ? dto.getReceiver() : null)
                .spender(dto.getSpender() != null ? dto.getSpender() : null)
                .name(dto.getName())
                .transactionType(dto.getTransactionType())
                .paymentType(dto.getPaymentType())
                .build();
    }

    public ExpensesDto toDto(Expenses expenses) {
        List<String> imagesUrls = new ArrayList<>();
        List<Image> imageList = expenses.getImages();
        imageList.forEach(image -> {
            imagesUrls.add(image.getUrl());
        });

        return ExpensesDto.builder()
                .id(expenses.getId())
                .amount(expenses.getAmount())
                .description(expenses.getDescription())
                .receiver(expenses.getReceiver())
                .name(expenses.getName())
                .spender(expenses.getSpender())
                .transactionType(expenses.getTransactionType())
                .paymentType(expenses.getPaymentType())
                .companyId(expenses.getCompany() != null ? expenses.getCompany().getId() : 1)
                .categoryName(expenses.getCategory().getName())
                .imageUrls(imagesUrls)
                .createdAt(expenses.getCreatedAt())
                .createdBy(expenses.getCreatedBy())
                .updatedAt(expenses.getUpdatedAt())
                .updatedBy(expenses.getUpdatedBy())
                .deleted(expenses.getDeleted())
                .build();
    }

    public List<ExpensesDto> dtoList(List<Expenses> list) {
        if (list != null && !list.isEmpty()) {
            return list.stream().map(this::toDto).toList();
        }
        return new ArrayList<>();
    }

    public Expenses toUpdate(Expenses expenses, ExpensesDto.UpdateExpense dto) {
        if (dto == null) return null;
        expenses.setName(getIfExists(dto.getName(), expenses.getName()));
        expenses.setReceiver(getIfExists(dto.getReceiver(), expenses.getReceiver()));
        expenses.setDescription(getIfExists(dto.getDescription(), expenses.getDescription()));
        expenses.setPaymentType(getIfExists(dto.getPaymentType(), expenses.getPaymentType()));
        return expenses;


    }
}
