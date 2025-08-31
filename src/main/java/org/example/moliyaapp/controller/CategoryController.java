package org.example.moliyaapp.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.example.moliyaapp.dto.ApiResponse;
import org.example.moliyaapp.dto.CategoryDto;
import org.example.moliyaapp.enums.CategoryStatus;
import org.example.moliyaapp.enums.TransactionType;
import org.example.moliyaapp.service.CategoryService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/category")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping("/create")
    public HttpEntity<ApiResponse> addCategory(@RequestBody CategoryDto.CategoryCreateDto dto) {
        ApiResponse apiResponse =  this.categoryService.addCategory(dto);
        return ResponseEntity.status(apiResponse.getStatus()).body(apiResponse);
    }

    @GetMapping("get-category/{id}")
    public ApiResponse getCategory(@PathVariable(name = "id") Long id) {
        return this.categoryService.getCategory(id);
    }

    @GetMapping("/get-all-categories")
    public ApiResponse getAllCategories(@RequestParam(value = "page", defaultValue = "0") int page,
                                        @RequestParam(value = "size", defaultValue = "10") int size,
                                        @RequestParam(value = "type", required = false)CategoryStatus categoryStatus) {
        return this.categoryService.getAllCategories(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC,"createdAt")), categoryStatus);
    }

    @GetMapping("/get-all-categories-list")
    public ApiResponse getAllCategoriesList(@RequestParam(value = "status", required = false) CategoryStatus categoryStatus) {
        return this.categoryService.getAllCategoriesList(categoryStatus);
    }


    @PutMapping("/update-category/{id}")
    public HttpEntity<ApiResponse> updateCategory(@PathVariable(name = "id") Long id, @RequestBody CategoryDto.CategoryCreateDto dto) {
        ApiResponse apiResponse= this.categoryService.updateCategory(id, dto);
        return ResponseEntity.status(apiResponse.getStatus()).body(apiResponse);
    }

    @Hidden
    @DeleteMapping("/delete-category/{id}")
    public ApiResponse deleteCategory(@PathVariable(name = "id") Long id) {
        return this.categoryService.deleteCategory(id);

    }
}
