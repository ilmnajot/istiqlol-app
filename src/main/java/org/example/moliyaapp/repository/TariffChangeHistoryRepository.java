package org.example.moliyaapp.repository;

import org.example.moliyaapp.entity.StudentContract;
import org.example.moliyaapp.entity.TariffChangeHistory;
import org.example.moliyaapp.enums.TariffStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TariffChangeHistoryRepository extends JpaRepository<TariffChangeHistory, Long> {

    @Query("select s from TariffChangeHistory s " +
            "where s.studentContract.id = :studentContractId and s.toDate is null")
    Optional<TariffChangeHistory> findActiveTariffByContractId(@Param("studentContractId") Long studentContractId);

    // Biror sana uchun qaysi tarif amal qilganini topish
    @Query("SELECT t FROM TariffChangeHistory t WHERE t.studentContract.id = :contractId AND :date BETWEEN t.fromDate AND COALESCE(t.toDate, CURRENT_DATE)")
    Optional<TariffChangeHistory> findTariffByDate(@Param("contractId") Long contractId,
                                                   @Param("date") LocalDate date);

    @Query("select t from TariffChangeHistory as t where t.studentContract.academicYear=:academicYear")
    List<TariffChangeHistory> findAllByAcademicYear(String academicYear);


    List<TariffChangeHistory> findAllByStudentContractId(@Param("id") Long studentContractId);

    @Query("select t from TariffChangeHistory as t where t.studentContract=:id and t.tariffStatus='YEARLY' and t.fromDate!=null and (t.toDate!=null or t.toDate is null)")
    List<TariffChangeHistory> findAllByStatusForYearly(@Param("id") Long id);
    // Find active tariff for a student at a specific date
    @Query("""
        SELECT tch FROM TariffChangeHistory tch 
        WHERE tch.studentContract.id = :studentContractId 
        AND tch.fromDate <= :date 
        AND (tch.toDate IS NULL OR tch.toDate >= :date)
        ORDER BY tch.fromDate DESC
        """)
    Optional<TariffChangeHistory> findActiveTariffAtDate(
            @Param("studentContractId") Long studentContractId,
            @Param("date") LocalDate date
    );
    @Query("""
        SELECT tch FROM TariffChangeHistory tch
        WHERE tch.studentContract.id = :studentContractId and tch.deleted=false and tch.toDate is null
        """)
    Optional<TariffChangeHistory> findActiveTariff(
            @Param("studentContractId") Long studentContractId
    );

    // Find all tariff changes for a student within a date range
    @Query("""
        SELECT tch FROM TariffChangeHistory tch 
        WHERE tch.studentContract.id = :studentContractId 
        AND (
            (tch.fromDate <= :endDate AND (tch.toDate IS NULL OR tch.toDate >= :startDate))
        )
        ORDER BY tch.fromDate ASC
        """)
    List<TariffChangeHistory> findByStudentContractIdAndDateRange(
            @Param("studentContractId") Long studentContractId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // Find all students who had a specific tariff status during a period
    @Query("""
        SELECT DISTINCT tch.studentContract FROM TariffChangeHistory tch 
        WHERE tch.tariffStatus = :tariffStatus 
        AND (
            (tch.fromDate <= :endDate AND (tch.toDate IS NULL OR tch.toDate >= :startDate))
        )
        """)
    List<StudentContract> findStudentContractsWithTariffStatusInPeriod(
            @Param("tariffStatus") TariffStatus tariffStatus,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // Find current active tariff for a student (where toDate is null)
    @Query("""
        SELECT tch FROM TariffChangeHistory tch 
        WHERE tch.studentContract.id = :studentContractId 
        AND tch.toDate IS NULL
        """)
    Optional<TariffChangeHistory> findCurrentActiveTariff(
            @Param("studentContractId") Long studentContractId
    );

    // Find all tariff histories for students in a specific academic year
    @Query("""
        SELECT tch FROM TariffChangeHistory tch 
        JOIN tch.studentContract sc 
        WHERE sc.academicYear = :academicYear 
        AND tch.tariffStatus = :tariffStatus
        ORDER BY tch.fromDate ASC
        """)
    List<TariffChangeHistory> findByAcademicYearAndTariffStatus(
            @Param("academicYear") String academicYear,
            @Param("tariffStatus") TariffStatus tariffStatus
    );
}
