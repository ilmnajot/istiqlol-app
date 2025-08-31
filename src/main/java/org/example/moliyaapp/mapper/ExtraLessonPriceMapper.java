package org.example.moliyaapp.mapper;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.moliyaapp.dto.ExtraLessonPriceDto;
import org.example.moliyaapp.entity.ExtraLessonPrice;
import org.example.moliyaapp.exception.ResourceNotFoundException;
import org.example.moliyaapp.repository.ExtraLessonPriceRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class ExtraLessonPriceMapper {
    private final ExtraLessonPriceRepository extraLessonPriceRepository;

    private boolean isPriceUsed(Long priceId) {
        return extraLessonPriceRepository.countExtraLessonPriceById(priceId) > 0;
    }

    public ExtraLessonPriceDto toDto(ExtraLessonPrice price) {
        if (price == null) {
            return null;
        }
        return ExtraLessonPriceDto.builder()
                .id(price.getId())
                .name(price.getName())
                .isUsed(isPriceUsed(price.getId()))
                .fixedAmount(price.getFixedAmount())
                .createdAt(price.getCreatedAt())
                .updatedAt(price.getUpdatedAt())
                .createdBy(price.getCreatedBy())
                .updatedBy(price.getUpdatedBy())
                .deleted(price.getDeleted())
                .build();
    }

    public ExtraLessonPrice toEntity(ExtraLessonPriceDto.CreatedAndUpdateDto dto) {
        return ExtraLessonPrice.builder()
                .name(dto.getName())
                .fixedAmount(dto.getFixedAmount())
                .build();
    }

    public void toUpdate(ExtraLessonPriceDto.CreatedAndUpdateDto dto, ExtraLessonPrice extraLessonPrice) {
        if (dto.getName() != null && !dto.getName().trim().isEmpty()) {
            extraLessonPrice.setName(dto.getName());
        }
        if (dto.getFixedAmount() != null && dto.getFixedAmount() > 0) {
            extraLessonPrice.setFixedAmount(dto.getFixedAmount());
        }
    }

    public List<ExtraLessonPriceDto> toDto(List<ExtraLessonPrice> list) {
        if (list != null && !list.isEmpty()) {
            return list.stream().map(this::toDto).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
