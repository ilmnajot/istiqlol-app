package org.example.moliyaapp.service;

import org.example.moliyaapp.dto.ApiResponse;
import org.example.moliyaapp.dto.CategoryDto;
import org.example.moliyaapp.enums.CategoryStatus;
import org.springframework.data.domain.Pageable;

public interface CategoryService {
    ApiResponse addCategory(CategoryDto.CategoryCreateDto dto);

    ApiResponse getCategory(Long id);

    ApiResponse deleteCategory(Long id);

    ApiResponse getAllCategories(Pageable pageable, CategoryStatus categoryStatus);

    ApiResponse getAllCategoriesList(CategoryStatus categoryStatus);

    ApiResponse updateCategory(Long id, CategoryDto.CategoryCreateDto dto);

}
