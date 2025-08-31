package org.example.moliyaapp.repository;

import org.example.moliyaapp.entity.StudentContract;
import org.example.moliyaapp.entity.Transaction;
import org.example.moliyaapp.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {


    @Query("SELECT t FROM Transaction t " +
            "JOIN t.monthlyFee mf " +
            "WHERE mf.studentContract.id IS NOT NULL")
    List<Transaction> findAllByExp();


    @Query(value = "select t.* from transaction as t join monthly_fee as mf on mf.id=t.monthly_fee_id where mf.student_contract_id is null and mf.employee_id is not null", nativeQuery = true)
    List<Transaction> findAllByEmployee();

    // Repository Query for Hourly Data
    @Query(value = """
            SELECT
                EXTRACT(HOUR FROM t.created_at) as hour,
                COALESCE(SUM(t.amount), 0) as amount,
                COUNT(t.id) as transaction_count
            FROM transaction t 
            WHERE t.transaction_type = ?1 
            AND DATE(t.created_at) = ?2 and extract(year from t.created_at) =?3
            GROUP BY EXTRACT(HOUR FROM t.created_at)
            ORDER BY EXTRACT(HOUR FROM t.created_at)
            """, nativeQuery = true)
    List<Object[]> getHourlyAmountRaw(String type, LocalDate date, Integer year);

    // Today amount
    @Query(value = """
                SELECT COALESCE(SUM(t.amount), 0)
                FROM transaction t
                WHERE t.transaction_type = ?1
                  AND DATE(t.created_at) = ?2
            """, nativeQuery = true)
    BigDecimal getTodayAmount(String type, LocalDate specificDate);

    // This week amount
    @Query(value = """
                SELECT COALESCE(SUM(t.amount), 0)
                FROM transaction t
                WHERE t.transaction_type = ?1
                  AND DATE_TRUNC('week', t.created_at) = DATE_TRUNC('week', CAST(?2 AS DATE))
            """, nativeQuery = true)
    BigDecimal getThisWeekAmount(String type, LocalDate targetDate);

    // This month amount
    @Query(value = """
                SELECT COALESCE(SUM(t.amount), 0)
                FROM transaction t
                WHERE t.transaction_type = ?1
                  AND EXTRACT(YEAR FROM t.created_at) = ?2
                  AND EXTRACT(MONTH FROM t.created_at) = ?3
            """, nativeQuery = true)
    BigDecimal getThisMonthAmount(String type, Integer year, Integer month);

    // This year amount
    @Query(value = """
                SELECT COALESCE(SUM(t.amount), 0)
                FROM transaction t
                WHERE t.transaction_type = ?1
                  AND EXTRACT(YEAR FROM t.created_at) = ?2
            """, nativeQuery = true)
    BigDecimal getYearAmount(String type, Integer year);


    @Query(value = "select sum(t.amount) from Transaction as t\n" +
            "where t.transaction_type=?1 and date(t.created_at)=?2", nativeQuery = true)
    BigDecimal getAllAmountByTypeADay(String type, LocalDate date);

    @Query(value = "select sum(t.amount) from transaction as t\n" +
            "where t.transaction_type=?1 and t.created_at between ?2 and ?3", nativeQuery = true)
    BigDecimal getAllAmountByTypeAWeek(String type, LocalDateTime start, LocalDateTime end);

    @Query(value = "select sum(t.amount) from transaction as t\n" +
            "where t.transaction_type=?1 and t.created_at between ?2 and ?3", nativeQuery = true)
    BigDecimal getAllAmountByTypeAMonths(String type, LocalDateTime start, LocalDateTime end);

    @Query(value = "select sum(t.amount) from transaction as t\n" +
            "where t.transaction_type=?1 and date_part('year',t.created_at)=?2", nativeQuery = true)
    BigDecimal getAllAmountByTypeAYear(String type, Integer year);


    @Query(value = """
                SELECT 
                    EXTRACT(MONTH FROM t.created_at) AS month_number,
                    t.transaction_type,
                    SUM(t.amount)
                FROM transaction t
                WHERE EXTRACT(YEAR FROM t.created_at) = :year
                GROUP BY month_number, t.transaction_type
                ORDER BY month_number
            """, nativeQuery = true)
    List<Object[]> getMonthlyAmountsByTypeAndYear(Integer year);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.transactionType = :name AND t.createdAt BETWEEN :startTime AND :endTime")
    BigDecimal getAllAmountByTypeBetweenDates(@Param("name") TransactionType name,
                                              @Param("startTime") LocalDateTime startTime,
                                              @Param("endTime") LocalDateTime endTime);

    @Query("select t from Transaction as t where t.monthlyFee.id=:monthlyFeeId and t.deleted=false")
    List<Transaction> findAllByMonthlyFeeId(@Param("monthlyFeeId") Long monthlyFeeId);


    Optional<Transaction> findByExpensesId(Long id);

    Optional<Transaction> findByIdAndDeletedFalse(Long transactionId);

    List<Transaction> findByMonthlyFeeStudentContractAndAmountAndTransactionType(StudentContract studentContract,
                                                                                 Double originalAmount,
                                                                                 TransactionType transactionType);

    // Yillik ma'lumot uchun - yil davomida har bir kun uchun umumiy summa
    @Query(value = """
            WITH week_days AS (
                SELECT 
                    CASE EXTRACT(DOW FROM t.created_at)
                        WHEN 1 THEN 'DUSHANBA'
                        WHEN 2 THEN 'SESHANBA'  
                        WHEN 3 THEN 'CHORSHANBA'
                        WHEN 4 THEN 'PAYSHANBA'
                        WHEN 5 THEN 'JUMA'
                        WHEN 6 THEN 'SHANBA'
                        WHEN 0 THEN 'YAKSHANBA'
                    END as day_name,
                    EXTRACT(DOW FROM t.created_at) as day_order,
                    COALESCE(SUM(t.amount), 0) as amount
                FROM transaction t
                WHERE t.transaction_type = ?1
                AND EXTRACT(YEAR FROM t.created_at) = ?2
                GROUP BY EXTRACT(DOW FROM t.created_at)
            )
            SELECT 
                CASE day_order 
                    WHEN 1 THEN 1
                    WHEN 2 THEN 2
                    WHEN 3 THEN 3
                    WHEN 4 THEN 4
                    WHEN 5 THEN 5
                    WHEN 6 THEN 6
                    WHEN 0 THEN 7
                END as day_id,
                day_name as label,
                amount
            FROM week_days
            ORDER BY CASE day_order 
                        WHEN 1 THEN 1
                        WHEN 2 THEN 2
                        WHEN 3 THEN 3
                        WHEN 4 THEN 4
                        WHEN 5 THEN 5
                        WHEN 6 THEN 6
                        WHEN 0 THEN 7
                     END
            """, nativeQuery = true)
    List<Object[]> getWeeklyDataForYearRaw(String type, Integer year);

    // Repository Query - har kun uchun ma'lumot (Dushanba dan Yakshanba gacha)
    @Query(value = """
            WITH week_days AS (
                SELECT 
                    CASE EXTRACT(DOW FROM t.created_at)
                        WHEN 1 THEN 'DUSHANBA'
                        WHEN 2 THEN 'SESHANBA'  
                        WHEN 3 THEN 'CHORSHANBA'
                        WHEN 4 THEN 'PAYSHANBA'
                        WHEN 5 THEN 'JUMA'
                        WHEN 6 THEN 'SHANBA'
                        WHEN 0 THEN 'YAKSHANBA'
                    END as day_name,
                    EXTRACT(DOW FROM t.created_at) as day_order,
                    COALESCE(SUM(t.amount), 0) as amount
                FROM transaction t
                WHERE t.transaction_type = ?1
                AND t.created_at::date BETWEEN ?2 AND ?3
                GROUP BY EXTRACT(DOW FROM t.created_at)
            )
            SELECT 
                CASE day_order 
                    WHEN 1 THEN 1
                    WHEN 2 THEN 2
                    WHEN 3 THEN 3
                    WHEN 4 THEN 4
                    WHEN 5 THEN 5
                    WHEN 6 THEN 6
                    WHEN 0 THEN 7
                END as day_id,
                day_name as label,
                amount
            FROM week_days
            ORDER BY CASE day_order 
                        WHEN 1 THEN 1
                        WHEN 2 THEN 2
                        WHEN 3 THEN 3
                        WHEN 4 THEN 4
                        WHEN 5 THEN 5
                        WHEN 6 THEN 6
                        WHEN 0 THEN 7
                     END
            """, nativeQuery = true)
    List<Object[]> getWeeklyDataForDateRangeRaw(String type, LocalDate fromDate, LocalDate toDate);

    // Repository queries
    @Query(value = """
            SELECT
                EXTRACT(DAY FROM t.created_at) as day_num,
                EXTRACT(DAY FROM t.created_at) || '-kun' as label,
                COALESCE(SUM(t.amount), 0) as amount
            FROM transaction t
            WHERE t.transaction_type = ?1
            AND EXTRACT(YEAR FROM t.created_at) = ?2
            AND EXTRACT(MONTH FROM t.created_at) = ?3
            GROUP BY EXTRACT(DAY FROM t.created_at)
            ORDER BY EXTRACT(DAY FROM t.created_at)
            """, nativeQuery = true)
    List<Object[]> getMonthlyDataForYearAndMonthRaw(String type, Integer year, Integer month);

    @Query(value = """
            SELECT
                EXTRACT(DAY FROM t.created_at) as day_num,
                EXTRACT(DAY FROM t.created_at) || '-kun' as label,
                COALESCE(SUM(t.amount), 0) as amount
            FROM transaction t
            WHERE t.transaction_type = ?1
            AND EXTRACT(YEAR FROM t.created_at) = ?2
            GROUP BY EXTRACT(DAY FROM t.created_at)
            ORDER BY EXTRACT(DAY FROM t.created_at)
            """, nativeQuery = true)
    List<Object[]> getMonthlyDataForYearRaw(String type, Integer year);

    @Query(value = """
            SELECT
                EXTRACT(DAY FROM t.created_at) as day_num,
                EXTRACT(DAY FROM t.created_at) || '-kun' as label,
                COALESCE(SUM(t.amount), 0) as amount
            FROM transaction t
            WHERE t.transaction_type = ?1
            AND t.created_at::date BETWEEN ?2 AND ?3
            GROUP BY EXTRACT(DAY FROM t.created_at)
            ORDER BY EXTRACT(DAY FROM t.created_at)
            """, nativeQuery = true)
    List<Object[]> getMonthlyDataForDateRangeRaw(String type, LocalDate fromDate, LocalDate toDate);

    @Query(value = """
            SELECT
                EXTRACT(MONTH FROM t.created_at) as month_num,
                CASE EXTRACT(MONTH FROM t.created_at)
                    WHEN 1 THEN 'YANVAR'
                    WHEN 2 THEN 'FEVRAL'
                    WHEN 3 THEN 'MART'
                    WHEN 4 THEN 'APREL'
                    WHEN 5 THEN 'MAY'
                    WHEN 6 THEN 'IYUN'
                    WHEN 7 THEN 'IYUL'
                    WHEN 8 THEN 'AVGUST'
                    WHEN 9 THEN 'SENTABR'
                    WHEN 10 THEN 'OKTABR'
                    WHEN 11 THEN 'NOYABR'
                    WHEN 12 THEN 'DEKABR'
                END as label,
                COALESCE(SUM(t.amount), 0) as amount
            FROM transaction t
            WHERE t.transaction_type = ?1
            AND EXTRACT(YEAR FROM t.created_at) = ?2
            GROUP BY EXTRACT(MONTH FROM t.created_at)
            ORDER BY EXTRACT(MONTH FROM t.created_at)
            """, nativeQuery = true)
    List<Object[]> getYearlyDataForYearRaw(String type, Integer year);

}
