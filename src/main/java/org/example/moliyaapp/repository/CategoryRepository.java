package org.example.moliyaapp.repository;

import org.example.moliyaapp.entity.Category;
import org.example.moliyaapp.enums.CategoryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByName(String name);

    Page<Category> findAllByAndCategoryStatus(Pageable pageable, CategoryStatus categoryStatus);

    @Query("select c from category c where c.categoryStatus = :categoryStatus")
    List<Category> findAllByCategoryStatus(CategoryStatus categoryStatus);

}
