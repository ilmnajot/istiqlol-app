package org.example.moliyaapp.service;

import jakarta.servlet.ServletOutputStream;
import org.example.moliyaapp.dto.ApiResponse;
import org.example.moliyaapp.dto.TeacherContractDto;
import org.example.moliyaapp.filter.TeacherContractFilter;
import org.example.moliyaapp.filter.TeacherContractSheetFilter;
import org.springframework.data.domain.Pageable;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.util.List;

public interface TeacherContractService {
    ApiResponse addTeacherContract(TeacherContractDto.CreateContractDto dto);

    ApiResponse getAllTeacherContractsList();

    ApiResponse getAllTeacherContractsPage(Pageable pageable);

    ApiResponse getTeacherContract(Long id);

    ApiResponse getContractsByTeacherId(Long teacherId);

    ApiResponse deleteTeacherContract(Long id);

    ApiResponse updateContract(TeacherContractDto.CreateContractDto dto, Long id);

    ApiResponse getActiveContractOfTeacher(Long teacherId);

    ApiResponse searchByName(String keyword, Pageable pageable);

    ApiResponse filter(TeacherContractFilter filter, Pageable pageable);

    ApiResponse deleteContracts(List<Long> ids);

    ApiResponse terminateAndActivateContract(Long contractId, LocalDate date, Boolean status);

    void downloadExcel(TeacherContractSheetFilter filter, OutputStream outputStream);

}
