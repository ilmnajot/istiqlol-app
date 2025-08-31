package org.example.moliyaapp.mapper;

import lombok.RequiredArgsConstructor;
import org.example.moliyaapp.dto.StudentTariffDto;
import org.example.moliyaapp.entity.StudentTariff;
import org.example.moliyaapp.repository.StudentTariffRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class StudentTariffMapper {

    private final StudentTariffRepository studentTariffRepository;

    private boolean isTariffUsed(Long tariffId) {
        return studentTariffRepository.countStudentTariffById(tariffId) > 0;
    }

    public StudentTariffDto toDto(StudentTariff studentTariff) {
        if (studentTariff == null) {
            return null;
        }
        return StudentTariffDto.builder()
                .id(studentTariff.getId())
                .name(studentTariff.getName())
                .amount(studentTariff.getAmount())
                .tariffStatus(studentTariff.getTariffStatus())
                .isUsed(isTariffUsed(studentTariff.getId()))
                .createdAt(studentTariff.getCreatedAt())
                .updatedAt(studentTariff.getUpdatedAt())
                .createdBy(studentTariff.getCreatedBy())
                .updatedBy(studentTariff.getUpdatedBy())
                .deleted(studentTariff.getDeleted())
                .build();
    }

    public StudentTariff toEntity(StudentTariffDto.TariffCreateDto studentTariffDto) {
        return StudentTariff.builder()
                .name(studentTariffDto.getName())
                .amount(studentTariffDto.getAmount())
                .tariffStatus(studentTariffDto.getTariffStatus())
                .build();
    }

    public void toUpdate(StudentTariffDto.TariffUpdateDto studentTariffDto, StudentTariff studentTariff) {
        if (studentTariffDto == null) return;
        if (studentTariffDto.getName() != null & !studentTariffDto.getName().trim().isEmpty()) {
            studentTariff.setName(studentTariffDto.getName());
        }
        if (studentTariffDto.getAmount() != null && studentTariffDto.getAmount() >= 0) {
            studentTariff.setAmount(studentTariffDto.getAmount());
        }
        if (studentTariffDto.getTariffStatus() != null) {
            studentTariff.setTariffStatus(studentTariffDto.getTariffStatus());
        }
    }

    public List<StudentTariffDto> toDto(List<StudentTariff> studentTariffs) {
        if (studentTariffs != null) {

            return studentTariffs
                    .stream()
                    .map(this::toDto)
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }


}
