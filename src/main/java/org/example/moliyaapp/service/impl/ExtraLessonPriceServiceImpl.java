package org.example.moliyaapp.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.moliyaapp.dto.ApiResponse;
import org.example.moliyaapp.dto.ExtraLessonPriceDto;
import org.example.moliyaapp.entity.ExtraLessonPrice;
import org.example.moliyaapp.exception.ResourceNotFoundException;
import org.example.moliyaapp.mapper.ExtraLessonPriceMapper;
import org.example.moliyaapp.repository.ExtraLessonPriceRepository;
import org.example.moliyaapp.service.ExtraLessonPriceService;
import org.example.moliyaapp.utils.RestConstants;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service

public class ExtraLessonPriceServiceImpl implements ExtraLessonPriceService {

    private final ExtraLessonPriceRepository extraLessonPriceRepository;
    private final ExtraLessonPriceMapper extraLessonPriceMapper;

    @Override
    public ApiResponse addFixedAmount(ExtraLessonPriceDto.CreatedAndUpdateDto dto) {
        Optional<ExtraLessonPrice> lessonPriceOptional = this.extraLessonPriceRepository.findByNameAndDeletedFalse(dto.getName());
        if (lessonPriceOptional.isPresent()) {
            return ApiResponse.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("QO'SHIMCHA DARS NOMI ALLAQACHON FOYDALANILGAN!")
                    .build();
        }
        ExtraLessonPrice extraLessonPrice = this.extraLessonPriceMapper.toEntity(dto);
        extraLessonPrice = this.extraLessonPriceRepository.save(extraLessonPrice);
        return ApiResponse.builder()
                .status(HttpStatus.CREATED)
                .message("SUCCESS")
                .data(this.extraLessonPriceMapper.toDto(extraLessonPrice))
                .build();
    }

    @Override
    public ApiResponse getFixedAmount(Long id) {
        ExtraLessonPrice extraLessonPrice = this.extraLessonPriceRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("QO'SHIMCHA TO'LOV TOPILMADI"));
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESS)
                .data(this.extraLessonPriceMapper.toDto(extraLessonPrice))
                .build();
    }

    @Override
    public ApiResponse getAllFixedAmountsPage(Pageable pageable) {
        Page<ExtraLessonPrice> pricePage = this.extraLessonPriceRepository.findAllByDeletedFalsePage(pageable);
        if (pricePage.isEmpty()) {
            return ApiResponse.builder()
                    .status(HttpStatus.OK)
                    .data(new ArrayList<>())
                    .build();
        }
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESS)
                .data(this.extraLessonPriceMapper.toDto(pricePage.getContent()))
                .elements(pricePage.getTotalElements())
                .pages(pricePage.getTotalPages())
                .build();
    }

    @Override
    public ApiResponse getAllFixedAmountsList() {
        List<ExtraLessonPrice> allByDeletedIsFalseList = this.extraLessonPriceRepository.findAllByDeletedIsFalseList();
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESS)
                .data(this.extraLessonPriceMapper.toDto(allByDeletedIsFalseList))
                .build();
    }

    @Override
    public ApiResponse updateFixedAmount(Long id, ExtraLessonPriceDto.CreatedAndUpdateDto dto) {
        ExtraLessonPrice extraLessonPrice = this.extraLessonPriceRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("QO'SHIMCHA DARS NARXI TOPILMADI"));
        this.extraLessonPriceMapper.toUpdate(dto, extraLessonPrice);
        this.extraLessonPriceRepository.save(extraLessonPrice);
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESSFULLY_UPDATED)
                .build();
    }

    @Override
    public ApiResponse deleteFixedAmount(Long id) {
        ExtraLessonPrice extraLessonPrice = this.extraLessonPriceRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("QO'SHIMCHA DARS NARXI TOPILMADI!"));
        extraLessonPrice.setDeleted(true);
        this.extraLessonPriceRepository.save(extraLessonPrice);
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message("MUVAFFAQIYATLI O'CHIRILDI")
                .build();
    }
}
