package org.example.moliyaapp.service;

import org.example.moliyaapp.dto.ApiResponse;
import org.example.moliyaapp.dto.StudentTariffDto;

public interface StudentTariffService {

    ApiResponse addStudentTariff(StudentTariffDto.TariffCreateDto dto);

    ApiResponse updateStudentTariff(Long id, StudentTariffDto.TariffUpdateDto dto);

    ApiResponse deleteStudentTariff(Long id);

    ApiResponse getStudentTariff(Long id);

    ApiResponse getAllStudentTariffs();

}
