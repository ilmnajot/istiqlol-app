package org.example.moliyaapp.service.impl;

import org.example.moliyaapp.dto.ApiResponse;
import org.example.moliyaapp.dto.CategoryDto;
import org.example.moliyaapp.entity.Category;
import org.example.moliyaapp.enums.CategoryStatus;
import org.example.moliyaapp.exception.ResourceNotFoundException;
import org.example.moliyaapp.mapper.CategoryMapper;
import org.example.moliyaapp.repository.CategoryRepository;
import org.example.moliyaapp.service.CategoryService;
import org.example.moliyaapp.utils.RestConstants;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Optional;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    public CategoryServiceImpl(CategoryRepository categoryRepository, CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
    }

    @Override
    public ApiResponse addCategory(CategoryDto.CategoryCreateDto dto) {

        Optional<Category> optionalCategory = this.categoryRepository.findByName(dto.getName());
        if (optionalCategory.isPresent()) {
            return ApiResponse.builder()
                    .message(RestConstants.CATEGORY_ALREADY_EXIST)
                    .status(HttpStatus.BAD_REQUEST)
                    .build();
        }
        Category entity = this.categoryMapper.toEntity(dto);
        entity = this.categoryRepository.save(entity);
        CategoryDto categoryDto = this.categoryMapper.toDto(entity);

        return ApiResponse.builder()
                .message(RestConstants.SUCCESS)
                .status(HttpStatus.CREATED)
                .data(categoryDto)
                .build();
    }

    @Override
    public ApiResponse getCategory(Long id) {

        Category category = this.categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.CATEGORY_NOT_FOUND));

        CategoryDto categoryDto = this.categoryMapper.toDto(category);

        return ApiResponse.builder()
                .message(RestConstants.SUCCESS)
                .status(HttpStatus.OK)
                .data(categoryDto)
                .build();
    }

    @Override
    public ApiResponse deleteCategory(Long id) {
        Category category = this.categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.CATEGORY_NOT_FOUND));
        category.setDeleted(true);
        this.categoryRepository.save(category);
        return ApiResponse.builder()
                .message(RestConstants.SUCCESSFULLY_DELETED)
                .status(HttpStatus.OK)
                .build();
    }

    @Override
    public ApiResponse getAllCategories(Pageable pageable, CategoryStatus categoryStatus) {
        Page<Category> all = this.categoryRepository.findAllByAndCategoryStatus(pageable, categoryStatus);
        if (!all.isEmpty()) {
            return ApiResponse.builder()
                    .message(RestConstants.SUCCESS)
                    .status(HttpStatus.OK)
                    .data(this.categoryMapper.toDto(all.getContent()))
                    .pages(all.getTotalPages())
                    .elements(all.getTotalElements())
                    .build();
        }
        return ApiResponse.builder()
                .message(RestConstants.SUCCESS)
                .status(HttpStatus.OK)
                .data(new ArrayList<>())
                .build();
    }

    @Override
    public ApiResponse getAllCategoriesList(CategoryStatus categoryStatus) {
        if (categoryStatus==null){
            return ApiResponse.builder()
                    .message(RestConstants.SUCCESS)
                    .status(HttpStatus.OK)
                    .data(this.categoryMapper.toDto(this.categoryRepository.findAll()))
                    .build();
        }
        return ApiResponse.builder()
                .message(RestConstants.SUCCESS)
                .status(HttpStatus.OK)
                .data(this.categoryMapper.toDto(this.categoryRepository.findAllByCategoryStatus(categoryStatus)))
                .build();
    }

    @Override
    public ApiResponse updateCategory(Long id, CategoryDto.CategoryCreateDto dto) {

        Category category = this.categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.CATEGORY_NOT_FOUND));

        this.categoryMapper.toUpdateEntity(dto, category);
        this.categoryRepository.save(category);

        return ApiResponse.builder()
                .message(RestConstants.SUCCESSFULLY_UPDATED)
                .status(HttpStatus.OK)
                .build();
    }
}
