package org.example.moliyaapp.service;

import org.example.moliyaapp.dto.ApiResponse;
import org.example.moliyaapp.dto.ExtraLessonPriceDto;
import org.springframework.data.domain.Pageable;

public interface ExtraLessonPriceService {

    ApiResponse addFixedAmount(ExtraLessonPriceDto.CreatedAndUpdateDto dto);

    ApiResponse getFixedAmount(Long id);

    ApiResponse getAllFixedAmountsPage(Pageable pageable);

    ApiResponse getAllFixedAmountsList();

    ApiResponse updateFixedAmount(Long id, ExtraLessonPriceDto.CreatedAndUpdateDto dto);

    ApiResponse deleteFixedAmount(Long id);

}
