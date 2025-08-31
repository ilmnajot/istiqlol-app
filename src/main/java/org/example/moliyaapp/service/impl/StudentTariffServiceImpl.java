package org.example.moliyaapp.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.moliyaapp.dto.ApiResponse;
import org.example.moliyaapp.dto.StudentTariffDto;
import org.example.moliyaapp.entity.StudentTariff;
import org.example.moliyaapp.exception.ResourceNotFoundException;
import org.example.moliyaapp.mapper.StudentTariffMapper;
import org.example.moliyaapp.repository.StudentTariffRepository;
import org.example.moliyaapp.service.StudentTariffService;
import org.example.moliyaapp.utils.RestConstants;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class StudentTariffServiceImpl implements StudentTariffService {

    private final StudentTariffRepository studentTariffRepository;
    private final StudentTariffMapper studentTariffMapper;


    @Override
    public ApiResponse addStudentTariff(StudentTariffDto.TariffCreateDto dto) {
        Optional<StudentTariff> tariffOptional = this.studentTariffRepository.findByName(dto.getName());
        if (tariffOptional.isPresent()) {
            return ApiResponse.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("Tarif allaqachon qo'shilgan!")
                    .build();
        }
        StudentTariff entity = this.studentTariffMapper.toEntity(dto);
        entity = this.studentTariffRepository.save(entity);
        StudentTariffDto tariffDto = this.studentTariffMapper.toDto(entity);
        return ApiResponse.builder()
                .status(HttpStatus.CREATED)
                .message(RestConstants.SUCCESSFULLY_SAVED)
                .data(tariffDto)
                .build();
    }

    @Override
    public ApiResponse updateStudentTariff(Long id, StudentTariffDto.TariffUpdateDto dto) {
        StudentTariff tariff = this.studentTariffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tarif topilmadi!"));
        this.studentTariffMapper.toUpdate(dto, tariff);
        this.studentTariffRepository.save(tariff);
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESSFULLY_UPDATED)
                .build();
    }

    @Override
    public ApiResponse deleteStudentTariff(Long id) {
        StudentTariff studentTariff = this.studentTariffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tarif topilmadi!"));
        studentTariff.setDeleted(true);
        this.studentTariffRepository.save(studentTariff);
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESSFULLY_DELETED)
                .build();
    }

    @Override
    public ApiResponse getStudentTariff(Long id) {
        StudentTariff studentTariff = this.studentTariffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tarif topilmadi!"));

        this.studentTariffRepository.save(studentTariff);
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESS)
                .data(this.studentTariffMapper.toDto(studentTariff))
                .build();
    }

    @Override
    public ApiResponse getAllStudentTariffs() {
        List<StudentTariff> studentTariffs = this.studentTariffRepository.findAll();
        return ApiResponse.builder()
                .status(HttpStatus.OK)
                .message(RestConstants.SUCCESS)
                .data(this.studentTariffMapper.toDto(studentTariffs))
                .build();
    }
}
