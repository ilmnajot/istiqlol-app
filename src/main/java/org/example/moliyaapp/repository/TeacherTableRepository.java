package org.example.moliyaapp.repository;

import org.example.moliyaapp.dto.TableDto;
import org.example.moliyaapp.entity.MonthlyFee;
import org.example.moliyaapp.entity.TeacherTable;
import org.example.moliyaapp.enums.Months;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface TeacherTableRepository extends JpaRepository<TeacherTable, Long>, JpaSpecificationExecutor<TeacherTable> {

    Optional<TeacherTable> findByIdAndDeletedFalse(Long id);

    @Query("select tt from teacher_table as tt where tt.deleted=false")
    Page<TeacherTable> findAllByDeletedFalsePage(Pageable pageable);

    List<TeacherTable> findAllByDeletedFalse();

    // Case: by single role
    @Query("SELECT tt FROM teacher_table tt JOIN tt.teacher t JOIN t.role r WHERE r.name = :role AND tt.deleted = false")
    Page<TeacherTable> findAllUsersByRole(@Param("role") String role, Pageable pageable);

    // Case: all NOT in (e.g., not teacher or employee)
    @Query("SELECT tt FROM teacher_table tt JOIN tt.teacher t JOIN t.role r WHERE r.name NOT IN :roles AND tt.deleted = false")
    Page<TeacherTable> findAllByRolesNotIn(@Param("roles") Set<String> roles, Pageable pageable);

    // Case: all IN (only teacher and employee)
    @Query("SELECT tt FROM teacher_table tt JOIN tt.teacher t JOIN t.role r WHERE r.name IN :roles AND tt.deleted = false")
    Page<TeacherTable> findAllByRolesIn(@Param("roles") Set<String> roles, Pageable pageable);


    @Query("select tt from teacher_table  as tt where tt.teacher.id=:teacherId and tt.months=:month order by tt.createdAt desc")
    List<TeacherTable> findByTeacherIdAndMonth(@Param("teacherId") Long teacherId, @Param("month") Months month);

    @Query("select count(tt.id) from teacher_table as tt " +
            "where tt.teacher.id = :teacherId " +
            "and tt.deleted = false")
    long findByTeacherIdAndYear(@Param("teacherId") Long teacherId);

    @Query(value = "SELECT *\n" +
            "FROM teacher_table AS tt\n" +
            "WHERE tt.deleted=false " +
            "and tt.teacher_id =:teacherId\n" +
            "  AND tt.months =:month\n" +
            "  AND date_part('YEAR',tt.created_at) =:year", nativeQuery = true)
    Optional<TeacherTable> existByTeacherIdAndMonthAndYear(@Param("teacherId") Long teacherId,
                                                           @Param("month") String month,
                                                           @Param("year") Integer year);

    @Query("select tt from teacher_table as tt where tt.months=:month and date_part('year', tt.createdAt)=:year")
    List<TeacherTable> findAllMonthsAndYear(@Param("month") Months month, @Param("year") Integer year);

    @Query("select tt from teacher_table as tt where tt.months=:month and date_part('year', tt.createdAt)=:year")
    Optional<TeacherTable> findByMonthsAndYear(@Param("month") Months month, @Param("year") Integer year);

    @Query("select tt from teacher_table as tt where tt.months=:month")
    Page<TeacherTable> findAllByMonths(@Param("month") Months month, Pageable pageable);

    @Query("select tt from teacher_table as tt where tt.deleted=false")
    Page<TeacherTable> findAllActiveTeacherTables(Pageable pageable);

    @Query("select tt from teacher_table as tt " +
            "where tt.deleted=false " +
            "and lower(tt.teacher.fullName)" +
            "like lower(concat('%',:keyword, '%') ) ")
    Page<TeacherTable> searchByName(@Param("keyword") String keyword, Pageable pageable);

    @Query("select tt from teacher_table as tt " +
            "where tt.teacherContract.id=:id " +
            "and tt.deleted=false")
    List<TeacherTable> findAllByTeacherContractId(@Param("id") Long id);

    @Query("select tt.teacherContract.id from teacher_table as tt where tt.teacherContract.id in :ids")
    List<Long> findAllByIds(@Param("ids") List<Long> ids);

    @Query("select tt.teacher.fullName from teacher_table as tt where tt.teacher.id in :userIds")
    List<String> findAllByUserIds(@Param("userIds") List<Long> userIds);

    @Query("select tt.id from teacher_table tt where tt.id in :ids and tt.deleted=true")
    List<Long> findAllByIdsAndDeletedTrue(@Param("ids") List<Long> ids);

    @Query("select tt from teacher_table tt where tt.id in :ids and tt.deleted=false")
    List<TeacherTable> findAllByIdsAndDeletedFalse(@Param("ids") List<Long> ids);

    @Transactional
    @Modifying
    @Query("delete from teacher_table as tt " +
            "where tt.deleted=true " +
            "and tt.id in :ids")
    void deleteAllByIds(@Param("ids") List<Long> ids);

    List<TeacherTable> findAllByTeacherIdAndDeletedFalse(Long teacherId);

    @Query("SELECT tt FROM teacher_table tt " +
            "WHERE tt.teacher.id = :teacherId " +
            "AND tt.months = :month " +
            "AND EXTRACT(YEAR FROM tt.createdAt) = :year")
    Optional<TeacherTable> findByTeacherIdAndMonthAndYear(
            @Param("teacherId") Long teacherId,
            @Param("month") Months month,
            @Param("year") int year
    );
}
