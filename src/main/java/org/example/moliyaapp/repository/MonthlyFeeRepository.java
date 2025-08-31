package org.example.moliyaapp.repository;

import jakarta.persistence.Tuple;
import org.example.moliyaapp.entity.MonthlyFee;
import org.example.moliyaapp.entity.StudentContract;
import org.example.moliyaapp.enums.CategoryStatus;
import org.example.moliyaapp.enums.Months;
import org.example.moliyaapp.enums.PaymentStatus;
import org.example.moliyaapp.enums.TariffStatus;
import org.example.moliyaapp.projection.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.Optional;

@Repository
public interface MonthlyFeeRepository extends JpaRepository<MonthlyFee, Long>, JpaSpecificationExecutor<MonthlyFee> {

    @Query(value = "select * from monthly_fee as mf where mf.status=?1", nativeQuery = true)
    Page<MonthlyFee> findAllByStatus(String status, Pageable pageable);

    @Query(value = "select * from monthly_fee as mf where mf.months=?1", nativeQuery = true)
    Page<MonthlyFee> findAllByMonths(String type, Pageable pageable);

    List<MonthlyFee> findAllByStudentContractId(Long studentContract_id);

    @Query(value = "select status,count(*) from monthly_fee group by status", nativeQuery = true)
    List<Tuple> countByStatus();

    Optional<MonthlyFee> findByIdAndDeletedFalse(Long id);

    Optional<MonthlyFee> findByStudentContractIdAndMonths(Long studentContractId, Months month);

    @Query("select m from MonthlyFee m join m.studentContract as sc " +
            "where m.studentContract.id = :studentContractId " +
            "and m.months = :month " +
            "and m.deleted = false " +
            "and sc.academicYear = :academicYear")
    Optional<MonthlyFee> findByStudentContractIdAndMonthsAndAcademicYear(@Param("studentContractId") Long studentContractId,
                                                                         @Param("month") Months month,
                                                                         @Param("academicYear") String academicYear);
    @Query("select m from MonthlyFee m join m.studentContract as sc " +
            "where m.studentContract.id = :studentContractId " +
            "and m.months = :month " +
            "and m.deleted = false " +
            "and sc.academicYear = :academicYear")
    List<MonthlyFee> findAllByStudentContractIdAndMonthsAndAcademicYear(@Param("studentContractId") Long studentContractId,
                                                                         @Param("month") Months month,
                                                                         @Param("academicYear") String academicYear);

    Optional<MonthlyFee> findByEmployeeIdAndMonths(Long employeeId, Months month);

    @Query(value = """
            SELECT * FROM monthly_fee mf
            WHERE mf.employee_id = ?1
              AND mf.months = ?2
              AND EXTRACT(YEAR FROM mf.created_at) = ?3
            """, nativeQuery = true)
    Optional<MonthlyFee> findByEmployeeIdAndMonthsAndYear(Long employeeId, String month, Integer year);

    @Query(value = "select *  from monthly_fee as mf  where mf.student_contract_id=?1 and mf.months=?2 order by mf.id desc limit 1",
            nativeQuery = true)
    Optional<MonthlyFee> findByContractIdAndMonthNameLast(Long contractId, String month);

    @Query(value = "" +
            "select *  from monthly_fee as mf  " +
            "where mf.employee_id=?1 " +
            "and mf.months=?2 " +
            "order by mf.id desc limit 1",
            nativeQuery = true)
    Optional<MonthlyFee> findByEmployeeIdAndMonthNameLast(Long employeeId, String month);

    List<MonthlyFee> findAllByEmployeeId(Long employeeId);

    @Query(value = """
                WITH months_list AS (
                    SELECT 'YANVAR' AS month, 1 AS month_num UNION
                    SELECT 'FEVRAL', 2 UNION
                    SELECT 'MART', 3 UNION
                    SELECT 'APREL', 4 UNION
                    SELECT 'MAY', 5 UNION
                    SELECT 'IYUN', 6 UNION
                    SELECT 'IYUL', 7 UNION
                    SELECT 'AVGUST', 8 UNION
                    SELECT 'SENTABR', 9 UNION
                    SELECT 'OKTABR', 10 UNION
                    SELECT 'NOYABR', 11 UNION
                    SELECT 'DEKABR', 12
                ),
                contract_months AS (
                    SELECT 
                        sc.id as contract_id,
                        sc.contract_start_date,
                        sc.contract_end_date,
                        st.id as tariff_id,
                        st.amount,
                        st.tariff_status,
                        -- Determine which months this contract should be charged for
                        CASE 
                            WHEN EXTRACT(MONTH FROM sc.contract_start_date) <= 9 THEN 9  -- September
                            ELSE EXTRACT(MONTH FROM sc.contract_start_date)
                        END as start_month_num,
                        CASE 
                            WHEN sc.contract_end_date IS NULL OR EXTRACT(MONTH FROM sc.contract_end_date) >= 6 THEN 6  -- June
                            ELSE EXTRACT(MONTH FROM sc.contract_end_date)
                        END as end_month_num
                    FROM student_contracts sc
                    JOIN student_tariff st ON sc.student_tariff_id = st.id
                    WHERE sc.academic_year = :academicYear
                    AND sc.status = true  -- Only active students
                )
                SELECT
                    UPPER(m.month) AS month,
                    COALESCE(SUM(
                        CASE 
                            WHEN mf.total_fee IS NOT NULL THEN mf.total_fee - COALESCE(mf.cut_amount, 0)
                            ELSE 
                                CASE cm.tariff_status
                                    WHEN 'YEARLY' THEN cm.amount / 10.0
                                    WHEN 'QUARTERLY' THEN cm.amount / 2.5
                                    WHEN 'MONTHLY' THEN cm.amount
                                    ELSE cm.amount
                                END
                        END
                    ), 0) AS totalAmount,
                    COALESCE(SUM(mf.amount_paid), 0) AS paidAmount,
                    COALESCE(SUM(
                        CASE 
                            WHEN mf.total_fee IS NOT NULL THEN mf.total_fee - COALESCE(mf.cut_amount, 0)
                            ELSE 
                                CASE cm.tariff_status
                                    WHEN 'YEARLY' THEN cm.amount / 10.0
                                    WHEN 'QUARTERLY' THEN cm.amount / 2.5
                                    WHEN 'MONTHLY' THEN cm.amount
                                    ELSE cm.amount
                                END
                        END
                    ), 0) - COALESCE(SUM(mf.amount_paid), 0) AS unPaidAmount,
                    COALESCE(SUM(mf.cut_amount), 0) AS cuttingAmount
                FROM months_list m
                CROSS JOIN contract_months cm
                LEFT JOIN monthly_fee mf ON cm.contract_id = mf.student_contract_id 
                    AND UPPER(mf.months) = UPPER(m.month)
                WHERE 
                    -- Only include months that fall within the contract period
                    (m.month_num >= cm.start_month_num AND m.month_num <= cm.end_month_num)
                    OR 
                    -- Handle academic year wrap-around (Sept-June)
                    (cm.start_month_num > cm.end_month_num AND 
                     (m.month_num >= cm.start_month_num OR m.month_num <= cm.end_month_num))
                GROUP BY UPPER(m.month), m.month_num
                ORDER BY 
                    CASE UPPER(m.month)
                        WHEN 'SENTABR' THEN 1
                        WHEN 'OKTABR' THEN 2
                        WHEN 'NOYABR' THEN 3
                        WHEN 'DEKABR' THEN 4
                        WHEN 'YANVAR' THEN 5
                        WHEN 'FEVRAL' THEN 6
                        WHEN 'MART' THEN 7
                        WHEN 'APREL' THEN 8
                        WHEN 'MAY' THEN 9
                        WHEN 'IYUN' THEN 10
                        WHEN 'IYUL' THEN 11
                        WHEN 'AVGUST' THEN 12
                        ELSE 13
                    END
            """, nativeQuery = true)
    List<GetAmountByMonth> getAcademicYearStats(@Param("academicYear") String academicYear);

    @Query(value = """
                WITH months_list AS (
                    SELECT 'SENTABR' AS month UNION
                    SELECT 'OKTABR' UNION
                    SELECT 'NOYABR' UNION
                    SELECT 'DEKABR' UNION
                    SELECT 'YANVAR' UNION
                    SELECT 'FEVRAL' UNION
                    SELECT 'MART' UNION
                    SELECT 'APREL' UNION
                    SELECT 'MAY' UNION
                    SELECT 'IYUN' UNION
                    SELECT 'IYUL' UNION
                    SELECT 'AVGUST'
                )
                SELECT
                    UPPER(m.month) AS month,
                    COALESCE(SUM(
                                case tc.salary_type 
                                            when 'PRICE_PER_MONTH' then tt.monthly_salary
                                            when 'PRICE_PER_LESSON' then tt.monthly_salary * COALESCE(tt.worked_days_or_lessons, 0)
                                            when 'PRICE_PER_DAY' then tt.monthly_salary * COALESCE(tt.worked_days_or_lessons, 0)
                                            else 0 
                                            END 
                                            ), 0) AS totalAmount,
                    COALESCE(SUM(tt.amount), 0) AS finalAmount,
                    COALESCE(SUM(mf.amount_paid), 0)  + COALESCE(SUM(mf.bonus), 0) AS paidAmount,
                    COALESCE(SUM(mf.remaining_balance), 0) AS unPaidAmount,
                    COALESCE(SUM(mf.bonus), 0) AS bonusAmount
                FROM months_list m
                CROSS JOIN teacher_table tt
                            JOIN teacher_contract tc ON tt.teacher_contract_id = tc.id
                                    AND tc.active = true
                LEFT JOIN monthly_fee mf ON tt.teacher_id = mf.employee_id 
                    AND UPPER(mf.months) = UPPER(m.month)
                WHERE UPPER(tt.months) = UPPER(m.month)
                    AND (mf.created_at IS NULL OR (mf.created_at >= :from AND mf.created_at <= :to))
                GROUP BY UPPER(m.month)
                ORDER BY 
                    CASE UPPER(m.month)
                        WHEN 'SENTABR' THEN 1
                        WHEN 'OKTABR' THEN 2
                        WHEN 'NOYABR' THEN 3
                        WHEN 'DEKABR' THEN 4
                        WHEN 'YANVAR' THEN 5
                        WHEN 'FEVRAL' THEN 6
                        WHEN 'MART' THEN 7
                        WHEN 'APREL' THEN 8
                        WHEN 'MAY' THEN 9
                        WHEN 'IYUN' THEN 10
                        WHEN 'IYUL' THEN 11
                        WHEN 'AVGUST' THEN 12
                        ELSE 13
                    END
            """, nativeQuery = true)
    List<GetEmployeeAmountByMonth> getAcademicYearStatsForEmployee(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = """
                WITH months_list AS (
                    SELECT 'SENTABR' AS month UNION
                    SELECT 'OKTABR' UNION
                    SELECT 'NOYABR' UNION
                    SELECT 'DEKABR' UNION
                    SELECT 'YANVAR' UNION
                    SELECT 'FEVRAL' UNION
                    SELECT 'MART' UNION
                    SELECT 'APREL' UNION
                    SELECT 'MAY' UNION
                    SELECT 'IYUN'UNION
                    SELECT 'IYUL' UNION 
                    SELECT 'AVGUST'
                )
                SELECT
                    UPPER(m.month) AS month,
                    COALESCE(SUM(
                                case tc.salary_type 
                                            when 'PRICE_PER_MONTH' then tt.monthly_salary
                                            when 'PRICE_PER_LESSON' then tt.monthly_salary * COALESCE(tt.worked_days_or_lessons, 0)
                                            when 'PRICE_PER_DAY' then tt.monthly_salary * COALESCE(tt.worked_days_or_lessons, 0)
                                            else 0 
                                            END 
                                            ), 0) AS totalAmount,
                    COALESCE(SUM(tt.amount), 0) AS finalAmount,
                    COALESCE(SUM(mf.amount_paid), 0) + COALESCE(SUM(mf.bonus), 0) AS paidAmount ,
                    COALESCE(SUM(mf.remaining_balance), 0) AS unPaidAmount,
                    COALESCE(SUM(mf.bonus), 0) AS bonusAmount
                FROM months_list m
                CROSS JOIN teacher_table tt
                            JOIN teacher_contract tc ON tt.teacher_contract_id = tc.id
                                    AND tc.active = true
                LEFT JOIN monthly_fee mf ON tt.teacher_id = mf.employee_id 
                    AND UPPER(mf.months) = UPPER(m.month)
                WHERE UPPER(tt.months) = UPPER(m.month)
                GROUP BY UPPER(m.month)
                ORDER BY 
                    CASE UPPER(m.month)
                        WHEN 'SENTABR' THEN 1
                        WHEN 'OKTABR' THEN 2
                        WHEN 'NOYABR' THEN 3
                        WHEN 'DEKABR' THEN 4
                        WHEN 'YANVAR' THEN 5
                        WHEN 'FEVRAL' THEN 6
                        WHEN 'MART' THEN 7
                        WHEN 'APREL' THEN 8
                        WHEN 'MAY' THEN 9
                        WHEN 'IYUN' THEN 10
                        WHEN 'IYUL' THEN 11
                        WHEN 'AVGUST' THEN 12
                        ELSE 11
                    END
            """, nativeQuery = true)
    List<GetEmployeeAmountByMonth> getAcademicYearStatsForEmployeeWithoutDateRange();


    @Query(value = """
                WITH months_list AS (
                    SELECT 'SENTABR' AS month UNION
                    SELECT 'OKTABR' UNION
                    SELECT 'NOYABR' UNION
                    SELECT 'DEKABR' UNION
                    SELECT 'YANVAR' UNION
                    SELECT 'FEVRAL' UNION
                    SELECT 'MART' UNION
                    SELECT 'APREL' UNION
                    SELECT 'MAY' UNION
                    SELECT 'IYUN' UNION
                    SELECT 'IYUL' UNION
                    SELECT 'AVGUST'
                )
                SELECT
                    UPPER(m.month) AS month,
                    COALESCE(SUM(
                                case tc.salary_type 
                                            when 'PRICE_PER_MONTH' then tt.monthly_salary
                                            when 'PRICE_PER_LESSON' then tt.monthly_salary * COALESCE(tt.worked_days_or_lessons, 0)
                                            when 'PRICE_PER_DAY' then tt.monthly_salary * COALESCE(tt.worked_days_or_lessons, 0)
                                            else 0 
                                            END 
                                            ), 0) AS totalAmount,
                    COALESCE(SUM(tt.amount), 0) AS finalAmount,
                    COALESCE(SUM(mf.amount_paid), 0) + COALESCE(SUM(mf.bonus), 0) AS paidAmount,
                    COALESCE(SUM(mf.remaining_balance), 0) AS unPaidAmount,
                    COALESCE(SUM(mf.bonus), 0) AS bonusAmount
                FROM months_list m
                CROSS JOIN teacher_table tt
                            JOIN teacher_contract tc ON tt.teacher_contract_id = tc.id
                                    AND tc.active = true
                LEFT JOIN monthly_fee mf ON tt.teacher_id = mf.employee_id 
                    AND UPPER(mf.months) = UPPER(m.month)
                WHERE UPPER(tt.months) = UPPER(m.month)
                    AND (mf.created_at IS NULL OR mf.created_at >= :from)
                GROUP BY UPPER(m.month)
                ORDER BY 
                    CASE UPPER(m.month)
                        WHEN 'SENTABR' THEN 1
                        WHEN 'OKTABR' THEN 2
                        WHEN 'NOYABR' THEN 3
                        WHEN 'DEKABR' THEN 4
                        WHEN 'YANVAR' THEN 5
                        WHEN 'FEVRAL' THEN 6
                        WHEN 'MART' THEN 7
                        WHEN 'APREL' THEN 8
                        WHEN 'MAY' THEN 9
                        WHEN 'IYUN' THEN 10
                        WHEN 'IYUL' THEN 11
                        WHEN 'AVGUST' THEN 12
                        ELSE 13
                    END
            """, nativeQuery = true)
    List<GetEmployeeAmountByMonth> getAcademicYearStatsForEmployeeWithFromDate(@Param("from") LocalDateTime from);

    @Query(value = """
                WITH months_list AS (
                    SELECT 'SENTABR' AS month UNION
                    SELECT 'OKTABR' UNION
                    SELECT 'NOYABR' UNION
                    SELECT 'DEKABR' UNION
                    SELECT 'YANVAR' UNION
                    SELECT 'FEVRAL' UNION
                    SELECT 'MART' UNION
                    SELECT 'APREL' UNION
                    SELECT 'MAY' UNION
                    SELECT 'IYUN' UNION
                    SELECT 'IYUL' UNION
                    SELECT 'AVGUST'
                )
                SELECT
                    UPPER(m.month) AS month,
                    COALESCE(SUM(
                                case tc.salary_type 
                                            when 'PRICE_PER_MONTH' then tt.monthly_salary
                                            when 'PRICE_PER_LESSON' then tt.monthly_salary * COALESCE(tt.worked_days_or_lessons, 0)
                                            when 'PRICE_PER_DAY' then tt.monthly_salary * COALESCE(tt.worked_days_or_lessons, 0)
                                            else 0 
                                            END 
                                            ), 0) AS totalAmount,
                    COALESCE(SUM(tt.amount), 0) AS finalAmount,
                    COALESCE(SUM(mf.amount_paid), 0) + COALESCE(SUM(mf.bonus), 0) AS paidAmount,
                    COALESCE(SUM(mf.remaining_balance), 0) AS unPaidAmount,
                    COALESCE(SUM(mf.bonus), 0) AS bonusAmount
                FROM months_list m
                CROSS JOIN teacher_table tt
                            JOIN teacher_contract tc ON tt.teacher_contract_id = tc.id
                                    AND tc.active = true
                LEFT JOIN monthly_fee mf ON tt.teacher_id = mf.employee_id 
                    AND UPPER(mf.months) = UPPER(m.month)
                WHERE UPPER(tt.months) = UPPER(m.month)
                    AND (mf.created_at IS NULL OR mf.created_at <= :to)
                GROUP BY UPPER(m.month)
                ORDER BY 
                    CASE UPPER(m.month)
                        WHEN 'SENTABR' THEN 1
                        WHEN 'OKTABR' THEN 2
                        WHEN 'NOYABR' THEN 3
                        WHEN 'DEKABR' THEN 4
                        WHEN 'YANVAR' THEN 5
                        WHEN 'FEVRAL' THEN 6
                        WHEN 'MART' THEN 7
                        WHEN 'APREL' THEN 8
                        WHEN 'MAY' THEN 9
                        WHEN 'IYUN' THEN 10
                        WHEN 'IYUL' THEN 11
                        WHEN 'AVGUST' THEN 12
                        ELSE 13
                    END
            """, nativeQuery = true)
    List<GetEmployeeAmountByMonth> getAcademicYearStatsForEmployeeWithToDate(@Param("to") LocalDateTime to);

    @Query("""
                SELECT
                    mf.months AS months,
                    mf.tariffName AS tariffName,
                    mf.totalFee AS totalFee,
                    mf.amountPaid AS amountPaid,
                    mf.remainingBalance AS remainingBalance
                FROM MonthlyFee mf join student_contracts as s on mf.studentContract.id = s.id
                WHERE mf.studentContract.id = :contractId and s.academicYear = :academicYear
            """)
    List<MonthlyPaymentInfo> getMonthlyPaymentInfosByContractId(@Param("contractId") Long contractId,
                                                                @Param("academicYear") String academicYear);


    @Query("SELECT mf FROM MonthlyFee mf JOIN mf.studentContract sc " +
            "WHERE mf.studentContract.id = :studentContractId " +
            "AND mf.months = :month " +
            "AND sc.academicYear = :academicYear")
    Optional<MonthlyFee> findByContractMonthAndAcademicYear(
            @Param("studentContractId") Long studentContractId,
            @Param("month") Months month,
            @Param("academicYear") String academicYear);


    @Query("select mf from MonthlyFee as mf where mf.studentContract.id=:id and mf.studentContract.academicYear=:academicYear and mf.months=:targetMonth")
    Optional<MonthlyFee> findByStudentContractAndMonthsAndAcademicYear(@Param("id") Long id, @Param("targetMonth") Months targetMonth, @Param("academicYear") String academicYear);


    // 1. MONTHLY tariff uchun - har oy bo'yicha
    @Query(value = """
            WITH months_list AS (
                SELECT 'YANVAR' AS month UNION
                SELECT 'FEVRAL' UNION
                SELECT 'MART' UNION
                SELECT 'APREL' UNION
                SELECT 'MAY' UNION
                SELECT 'IYUN' UNION
                SELECT 'SENTABR' UNION
                SELECT 'OKTABR' UNION
                SELECT 'NOYABR' UNION
                SELECT 'DEKABR'
            )
            SELECT
                UPPER(m.month) AS month,
                COALESCE(SUM(
                    CASE 
                        WHEN mf.total_fee IS NOT NULL THEN mf.total_fee - COALESCE(mf.cut_amount, 0)
                        ELSE COALESCE(sc.amount, st.amount)
                    END
                ), 0) AS totalAmount,
                COALESCE(SUM(mf.amount_paid), 0) AS paidAmount,
                COALESCE(SUM(
                    CASE 
                        WHEN mf.total_fee IS NOT NULL THEN mf.total_fee - COALESCE(mf.cut_amount, 0)
                        ELSE COALESCE(sc.amount, st.amount)
                    END
                ), 0) - COALESCE(SUM(mf.amount_paid), 0) AS unPaidAmount,
                COALESCE(SUM(mf.cut_amount), 0) AS cuttingAmount
            FROM months_list m
            CROSS JOIN student_contracts sc
            JOIN student_tariff st ON sc.student_tariff_id = st.id
            LEFT JOIN monthly_fee mf ON sc.id = mf.student_contract_id 
                AND UPPER(mf.months) = UPPER(m.month)
            WHERE sc.academic_year = :academicYear
                AND st.tariff_status = :tariffStatus
                AND sc.status = true
            GROUP BY UPPER(m.month)
            ORDER BY 
                CASE UPPER(m.month)
                    WHEN 'SENTABR' THEN 1
                    WHEN 'OKTABR' THEN 2
                    WHEN 'NOYABR' THEN 3
                    WHEN 'DEKABR' THEN 4
                    WHEN 'YANVAR' THEN 5
                    WHEN 'FEVRAL' THEN 6
                    WHEN 'MART' THEN 7
                    WHEN 'APREL' THEN 8
                    WHEN 'MAY' THEN 9
                    WHEN 'IYUN' THEN 10
                    ELSE 11
                END
            """, nativeQuery = true)
    List<GetAmountByMonth> getStatsByTariffMonthly(@Param("academicYear") String academicYear,
                                                   @Param("tariffStatus") String tariffStatus);

    @Query(value = """
            WITH quarters_list AS (
                SELECT 'Q1' AS quarter, 'SENTABR,OKTABR,NOYABR' AS months UNION
                SELECT 'Q2', 'DEKABR,YANVAR,FEVRAL' UNION
                SELECT 'Q3', 'MART,APREL,MAY' UNION
                SELECT 'Q4', 'IYUN' AS months
            )
            SELECT
                q.quarter,
                COALESCE(SUM(
                    CASE 
                        WHEN mf.total_fee IS NOT NULL THEN mf.total_fee - COALESCE(mf.cut_amount, 0)
                        ELSE COALESCE(sc.amount, st.amount / 2.5)
                    END
                ), 0) AS totalAmount,
                COALESCE(SUM(mf.amount_paid), 0) AS paidAmount,
                COALESCE(SUM(
                    CASE 
                        WHEN mf.total_fee IS NOT NULL THEN mf.total_fee - COALESCE(mf.cut_amount, 0)
                        ELSE COALESCE(sc.amount, st.amount / 2.5)
                    END
                ), 0) - COALESCE(SUM(mf.amount_paid), 0) AS unPaidAmount,
                COALESCE(SUM(mf.cut_amount), 0) AS cuttingAmount
            FROM quarters_list q
            CROSS JOIN student_contracts sc
            JOIN student_tariff st ON sc.student_tariff_id = st.id
            LEFT JOIN monthly_fee mf ON sc.id = mf.student_contract_id 
                AND (
                    (q.quarter = 'Q1' AND mf.months IN ('SENTABR', 'OKTABR', 'NOYABR')) OR
                    (q.quarter = 'Q2' AND mf.months IN ('DEKABR', 'YANVAR', 'FEVRAL')) OR
                    (q.quarter = 'Q3' AND mf.months IN ('MART', 'APREL', 'MAY')) OR
                    (q.quarter = 'Q4' AND mf.months IN ('IYUN'))
                )
            WHERE sc.academic_year = :academicYear
                AND st.tariff_status = :tariffStatus
                AND sc.status = true
            GROUP BY q.quarter
            ORDER BY q.quarter
            """, nativeQuery = true)
    List<GetAmountByQuarter> getStatsByTariffQuarterly(@Param("academicYear") String academicYear,
                                                       @Param("tariffStatus") String tariffStatus);

    @Query(value = "select mf.* from monthly_fee as mf" +
            " where mf.employee_id is not null " +
            "and mf.student_contract_id is null",
            nativeQuery = true)
    List<MonthlyFee> findAllEmployeeSheet();
}

