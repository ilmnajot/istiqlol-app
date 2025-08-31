package org.example.moliyaapp.repository;

import jakarta.transaction.Transactional;
import org.example.moliyaapp.entity.TeacherContract;
import org.example.moliyaapp.filter.TeacherContractSheetFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherContractRepository extends JpaRepository<TeacherContract, Long>, JpaSpecificationExecutor<TeacherContract> {

    @Query("select tc from teacher_contract as tc " +
            "where tc.teacher.id=:id " +
            "and tc.deleted = false " +
            "and tc.active=true")
    Optional<TeacherContract> findByTeacherIdAndDeletedFalse(@Param("id") Long id);
    @Query("select tc from teacher_contract as tc " +
            "where tc.teacher.id=:id " +
            "and tc.deleted = false " +
            "and tc.active=true")
    List<TeacherContract> findByTeacherIdAndActiveTrue(@Param("id") Long id);

    List<TeacherContract> findByTeacherId(Long id);

    Optional<TeacherContract> findByIdAndDeletedFalse(Long id);

    @Query("select c from teacher_contract as c " +
            "where c.id=:id " +
            "and c.deleted=false")
    Optional<TeacherContract> findByIdAndDFalseAndActive(@Param("id") Long id);

    //    @Query("select tc from teacher_contract as tc where tc.deleted=false")
    Page<TeacherContract> findAllByDeletedIsFalse(Pageable pageable);

    //    @Query("select tc from teacher_contract as tc where tc.deleted=false")
    List<TeacherContract> findAllByDeletedIsFalse();

    @Query("""
                select tc from teacher_contract as tc 
                where tc.teacher.id = :teacherId 
                  and tc.startDate <= CURRENT_DATE 
                  and (tc.endDate is null or tc.endDate >= CURRENT_DATE) 
                  and tc.deleted = false
            """)
    Optional<TeacherContract> findActiveContractByTeacherId(Long teacherId);

    @Query("select tc from teacher_contract as tc " +
            "where tc.teacher.id=:userId " +
            "and tc.deleted=false " +
            "and tc.active=true")
    List<TeacherContract> findAllByIdAndActiveTrueAndDeletedFalse(@Param("userId") Long userId);

    @Query("select tc from teacher_contract as tc " +
            "where tc.deleted=false " +
            "and lower(tc.teacher.fullName) " +
            "like lower(concat('%', :keyword,'%'))")
    Page<TeacherContract> searchByName(String keyword, Pageable pageable);

    @Query("select tc.id from teacher_contract as tc " +
            "where tc.deleted=true " +
            "and tc.active=false " +
            "and tc.id in :ids")
    List<Long> findAllByIdsAndDeletedTrue(@Param("ids") List<Long> ids);

    @Transactional
    @Modifying
    @Query("delete from teacher_contract tc where tc.id in :ids ")
    void deleteAllByIds(@Param("ids") List<Long> ids);

    @Query("select tc.teacher.fullName from teacher_contract as tc where tc.teacher.id in :userIds")
    List<String> findAllByUserIds(@Param("userIds") List<Long> userIds);

    @Query("SELECT tc FROM teacher_contract tc WHERE tc.endDate <= :date AND tc.active = true")
    List<TeacherContract> findExpiredContracts(@Param("date") LocalDate date);

}
