package org.example.moliyaapp.controller;

import lombok.RequiredArgsConstructor;
import org.example.moliyaapp.dto.ApiResponse;
import org.example.moliyaapp.dto.ExtraLessonPriceDto;
import org.example.moliyaapp.service.ExtraLessonPriceService;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/extra-lesson-price")
public class ExtraLessonAmountController {
    private final ExtraLessonPriceService extraLessonPriceService;

    @PreAuthorize("hasAnyRole('DEVELOPER','ADMIN')")
    @PostMapping("/add-fixed-amount")
    public ApiResponse addFixedAmount(@RequestBody ExtraLessonPriceDto.CreatedAndUpdateDto dto) {
        return this.extraLessonPriceService.addFixedAmount(dto);
    }
    @PreAuthorize("hasAnyRole('DEVELOPER','ADMIN')")
    @PutMapping("/update-fixedAmount/{id}")
    public ApiResponse updateFixedAmount(@PathVariable(name = "id") Long id, @RequestBody ExtraLessonPriceDto.CreatedAndUpdateDto dto) {
        return this.extraLessonPriceService.updateFixedAmount(id, dto);
    }

    @PreAuthorize("hasAnyRole('DEVELOPER','ADMIN','OWNER','PRINCIPAL','HR','EDUCATIONAL_DEPARTMENT')")
    @GetMapping("/get-fixedAmount/{id}")
    public ApiResponse getFixedAmount(@PathVariable(name = "id") Long id) {
        return this.extraLessonPriceService.getFixedAmount(id);
    }

    @PreAuthorize("hasAnyRole('DEVELOPER','ADMIN','OWNER','PRINCIPAL','HR','EDUCATIONAL_DEPARTMENT')")
    @GetMapping("/get-fixedAmount-page")
    public ApiResponse getFixedAmountPage(@RequestParam(value = "page", defaultValue = "0") int page,
                                          @RequestParam(value = "size",defaultValue = "10") int size) {
        return this.extraLessonPriceService.getAllFixedAmountsPage(PageRequest.of(page, size));
    }

    @PreAuthorize("hasAnyRole('DEVELOPER','ADMIN','OWNER','PRINCIPAL','HR','EDUCATIONAL_DEPARTMENT')")
    @GetMapping("/get-fixedAmount-list")
    public ApiResponse getFixedAmountList() {
        return this.extraLessonPriceService.getAllFixedAmountsList();
    }
    @PreAuthorize("hasAnyRole('DEVELOPER','ADMIN')")
    @DeleteMapping("/delete-fixedAmount/{id}")
    public ApiResponse deleteFixedAmount(@PathVariable Long id) {
        return this.extraLessonPriceService.deleteFixedAmount(id);
    }
}
