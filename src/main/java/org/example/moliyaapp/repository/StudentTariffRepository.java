package org.example.moliyaapp.repository;

import org.example.moliyaapp.entity.StudentTariff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentTariffRepository extends JpaRepository<StudentTariff, Long> {


    Optional<StudentTariff> findByName(String name);

    Optional<StudentTariff> findByNameIgnoreCase(String tariffName);

    @Query("select count(s.id) from student_contracts s where s.id =:id and s.deleted = false")
    long countStudentTariffById(@Param("id") Long id);

    Optional<StudentTariff> findByIdAndDeletedFalse(Long tariffId);
}
