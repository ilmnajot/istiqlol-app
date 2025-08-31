package org.example.moliyaapp.mapper;

import org.example.moliyaapp.dto.CategoryDto;
import org.example.moliyaapp.entity.Category;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CategoryMapper {

    public CategoryDto toDto(Category category) {
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .categoryStatus(category.getCategoryStatus())
                .createdAt(category.getCreatedAt())
                .createdBy(category.getCreatedBy())
                .updatedAt(category.getUpdatedAt())
                .updatedBy(category.getUpdatedBy())
                .deleted(category.getDeleted())
                .build();
    }

    public Category toEntity(CategoryDto.CategoryCreateDto dto) {
        return Category.builder()
                .name(dto.getName())
                .categoryStatus(dto.getCategoryStatus())
                .build();
    }


    public void toUpdateEntity(CategoryDto.CategoryCreateDto dto, Category entity) {
        if (dto.getName() != null && !dto.getName().isEmpty()) {
            entity.setName(dto.getName());
        }
        if (dto.getCategoryStatus()!=null){
            entity.setCategoryStatus(dto.getCategoryStatus());
        }
    }

    public List<CategoryDto> toDto(List<Category> categories) {
        if (categories != null && !categories.isEmpty()) {
            return categories.stream().map(this::toDto).toList();
        }
        return new ArrayList<>();
    }

    public void toUpdate(Category category, CategoryDto.CategoryCreateDto dto) {
        if (dto.getName() != null && !dto.getName().trim().isEmpty()) {
            category.setName(dto.getName());
        }
    }

}
