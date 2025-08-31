package org.example.moliyaapp.repository;

import org.example.moliyaapp.entity.StudentContract;
import org.example.moliyaapp.enums.Grade;
import org.example.moliyaapp.enums.StudentGrade;
import org.example.moliyaapp.enums.TariffStatus;
import org.example.moliyaapp.projection.GetAmountAndCount;
import org.example.moliyaapp.projection.GetStudentsByGender;
import org.example.moliyaapp.projection.GetStudentsByGrade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentContractRepository extends JpaRepository<StudentContract, Long>, JpaSpecificationExecutor<StudentContract> {

    @Query("select s from student_contracts as s where (s.deleted is false or s.deleted is null) and s.id=:id")
    Optional<StudentContract> findByIdAndDeletedFalse(@Param("id") Long id);
    @Query("select s from student_contracts as s where s.deleted=false and s.BCN=:BCN and s.academicYear=:academicYear")
    Optional<StudentContract> findByBCN(@Param("BCN") String BCN, @Param("academicYear") String academicYear);

    @Query("select s from student_contracts as s where (:grade is null or s.grade=:grade) and (s.deleted=false or s.deleted is null)")
    List<StudentContract> findByGrade(@Param("grade") StudentGrade grade);

    @Query("select sc from student_contracts as sc " +
            "where COALESCE (:fromDate, sc.contractedDate)<=sc.contractedDate " +
            "and COALESCE(:toDate, sc.contractedDate)>=sc.contractedDate ")
    Page<StudentContract> getAllByDate(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate, Pageable pageable);

    @Query("SELECT sc FROM student_contracts sc WHERE :status IS NULL OR sc.status = :status OR (sc.status IS NULL AND :status IS NULL)")
    Page<StudentContract> findAllByStatus(@Param("status") Boolean status, Pageable pageable);

    Page<StudentContract> findAllByDeletedIsTrue(Pageable pageable);

    @Query(value = "select * from student_contracts where status=?1", nativeQuery = true)
    List<StudentContract> findAllByStatus(Boolean status);

    List<StudentContract> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT COUNT(sc.id) FROM student_contracts sc WHERE sc.createdAt BETWEEN :start AND :end")
    Integer getTotalStudentContracts(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);


    @Query("SELECT COUNT(sc.id) " +
            "FROM student_contracts sc " +
            "WHERE sc.createdAt " +
            "BETWEEN :start " +
            "AND :end " +
            "and sc.status=:status")
    Integer getTotalStudentContractsStatistics(@Param("start") LocalDateTime start,
                                               @Param("end") LocalDateTime end, @Param("status") Boolean status);


    @Query(value = "\n" +
            "select count(sc.id) " +
            "from student_contracts as sc " +
            "where sc.deleted=false " +
            "and sc.status=?1 " +
            "and EXTRACT(YEAR FROM sc.created_at)=?2", nativeQuery = true)
    Integer getAllByCurrentYear(Boolean status, Integer year);

    @Query("select sc.id from student_contracts as sc " +
            "where sc.id in :ids" +
            " and sc.deleted=true")
    List<Long> findAllByIdsAndDeletedTure(@Param("ids") List<Long> ids);

    @Query("select sc from student_contracts as sc " +
            "where sc.id in :ids" +
            " and sc.deleted=false")
    List<StudentContract> findAllByIdsAndDeletedFalse(@Param("ids") List<Long> ids);


    @Modifying
    @Transactional
    @Query("delete from student_contracts as sc where sc.id in :ids")
    void deleteALlByIds(@Param("ids") List<Long> ids);

    @Query("SELECT s.gender AS gender, COUNT(s.id) AS count " +
            "FROM student_contracts as s " +
            "WHERE s.deleted=false " +
            "and  s.academicYear = :academicYear " +
            "and s.status=:status " +
            "GROUP BY s.gender")
    List<GetStudentsByGender> findAllByGender(
            @Param("academicYear") String academicYear,
            @Param("status") Boolean status);

    @Query(value = """
            SELECT 
                grade as studentGrade,
                st_grade as grade,
                language as studyLanguage,
                COUNT(CASE WHEN gender = 'MALE' THEN 1 END) as maleCount,
                COUNT(CASE WHEN gender = 'FEMALE' THEN 1 END) as femaleCount,
                COUNT(CASE WHEN client_type = 'OLD' THEN 1 END) as oldCount,
                COUNT(CASE WHEN client_type = 'NEW' THEN 1 END) as newCount
            FROM student_contracts as s 
            WHERE academic_year = :academicYear 
                        and s.status = :status
                     --AND (:from IS NULL OR :to IS NULL OR s.created_at BETWEEN :from AND :to)
                          AND s.created_at BETWEEN
                            COALESCE(:from, TIMESTAMP '1900-01-01 00:00:00')
                            AND COALESCE(:to, TIMESTAMP '2100-12-31 23:59:59')
            GROUP BY grade, st_grade, language
            ORDER BY 
                CAST(SUBSTRING(grade FROM 7) AS INTEGER),  -- Extract number from GRADE_1
                st_grade ASC,
                language ASC
            """, nativeQuery = true)
    List<GetStudentsByGrade> findAllByGrade(@Param("academicYear") String academicYear,
                                            @Param("status") Boolean status,
                                            @Param("from") LocalDateTime from,
                                            @Param("to") LocalDateTime to);

    @Query("SELECT t.name AS tariffName, " +
            "t.amount * COUNT(s.id) AS amount, " +
            "COUNT(s.id) AS count " +
            "FROM student_contracts s " +
            "INNER JOIN StudentTariff t ON s.tariff.id = t.id " +
            "WHERE s.academicYear = :academicYear AND s.status = true " +
            "GROUP BY t.name, t.amount")
    List<GetAmountAndCount> findAllByTariff(@Param("academicYear") String academicYear);

    @Query(value = """
            
                    SELECT
                d.day_num AS day,
                COALESCE(COUNT(sc.id), 0) AS count
            FROM
                -- Generate a series of days for the given month and year
                GENERATE_SERIES(
                    MAKE_DATE(:year, :month, 1),
                    (MAKE_DATE(:year, :month, 1) + INTERVAL '1 month - 1 day')::DATE,
                    '1 day'::INTERVAL
                ) AS s(date_col)
            JOIN LATERAL (SELECT EXTRACT(DAY FROM s.date_col)::INTEGER AS day_num) d ON TRUE -- Extract day number from generated date
            LEFT JOIN
                student_contracts sc ON EXTRACT(DAY FROM sc.contracted_date) = d.day_num
                                     AND EXTRACT(MONTH FROM sc.contracted_date) = :month
                                     AND EXTRACT(YEAR FROM sc.contracted_date) = :year
            GROUP BY
                d.day_num
            ORDER BY
                d.day_num;
            """, nativeQuery = true)
    List<Object[]> getDailyContractCountsForMonth(@Param("month") int month, @Param("year") int year);


    @Query("SELECT tc FROM student_contracts tc WHERE tc.contractEndDate <= :date AND tc.status = true")
    List<StudentContract> findExpiredContracts(@Param("date") LocalDate date);


    Optional<StudentContract> findByIdAndDeletedFalseAndStatusTrue(Long id);


    @Query("select s from student_contracts as s where s.academicYear=:academicYear")
    List<StudentContract> findAllByAcademicYear(@Param("academicYear") String academicYear);

}


