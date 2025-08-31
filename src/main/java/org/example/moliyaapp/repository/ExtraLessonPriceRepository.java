package org.example.moliyaapp.repository;

import org.example.moliyaapp.entity.ExtraLessonPrice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExtraLessonPriceRepository extends JpaRepository<ExtraLessonPrice, Long> {
    Optional<ExtraLessonPrice> findByNameAndDeletedFalse(String name);

    Optional<ExtraLessonPrice> findByIdAndDeletedFalse(Long id);

    @Query("select e from ExtraLessonPrice as e where e.deleted =false")
    Page<ExtraLessonPrice> findAllByDeletedFalsePage(Pageable pageable); @Query("select e from ExtraLessonPrice as e where e.deleted =false")
    List<ExtraLessonPrice> findAllByDeletedIsFalseList();

//    @Query("select count(e.id) from ExtraLessonPrice e where e.id =:id and e.deleted = false")
//    long countExtraLessonPriceById(@Param("id") Long id);

    @Query("SELECT COUNT(l) FROM teacher_table l WHERE l.extraLessonPrice.id = :priceId AND l.deleted = false")
    long countExtraLessonPriceById(@Param("priceId") Long priceId);
}
