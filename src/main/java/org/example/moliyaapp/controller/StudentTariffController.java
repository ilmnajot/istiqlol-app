package org.example.moliyaapp.controller;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.example.moliyaapp.dto.ApiResponse;
import org.example.moliyaapp.dto.StudentTariffDto;
import org.example.moliyaapp.service.StudentTariffService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;

@RequiredArgsConstructor
@RestController
@RequestMapping("/studentTariffs")
public class StudentTariffController {

    private final StudentTariffService studentTariffService;

    @PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN')")
    @PostMapping("/add-studentTariff")
    public ApiResponse addStudentTariff(@RequestBody StudentTariffDto.TariffCreateDto dto) {
        return this.studentTariffService.addStudentTariff(dto);
    }

    @PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN')")
    @PutMapping("/update-studentTariff/{tariffId}")
    public HttpEntity<ApiResponse> updateTariff(@PathVariable(value = "tariffId") Long tariffId,
                                                @RequestBody StudentTariffDto.TariffUpdateDto dto) {
        ApiResponse apiResponse = this.studentTariffService.updateStudentTariff(tariffId, dto);
        return ResponseEntity.status(apiResponse.getStatus()).body(apiResponse);
    }

    @PreAuthorize("hasAnyRole('DEVELOPER', 'OWNER', 'PRINCIPAL', 'ADMIN','RECEPTION','EDUCATIONAL_DEPARTMENT','STUDENT_CONTRACT_MAKER')")
    @GetMapping("/get-studentTariffs")
    public ApiResponse getAllStudentTariffs() {
        return this.studentTariffService.getAllStudentTariffs();
    }

    @GetMapping("/get-studentTariff/{tariffId}")
    public ApiResponse getStudentTariff(@PathVariable(value = "tariffId") Long tariffId) {
        return this.studentTariffService.getStudentTariff(tariffId);
    }

    @PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN')")
    @Hidden
    @DeleteMapping("/delete-studentTariff/{id}")
    public ApiResponse deleteStudentTariff(@PathVariable(value = "id") Long tariffId) {
        return this.studentTariffService.deleteStudentTariff(tariffId);
    }
}
