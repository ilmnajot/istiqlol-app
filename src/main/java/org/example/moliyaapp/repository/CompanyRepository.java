package org.example.moliyaapp.repository;

import org.example.moliyaapp.dto.CompanyDto;
import org.example.moliyaapp.entity.Company;
import org.example.moliyaapp.projection.TransactionSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {

    @Query(value = "select * from company", nativeQuery = true)
    Optional<Company> findOne();

    @Query(value = """
    SELECT 
        t.transaction_type AS transactionType,
        SUM(t.amount) AS amount,
        SUM(CASE WHEN t.payment_type = 'NAQD' THEN t.amount ELSE 0 END) AS cashAmount,
        SUM(CASE WHEN t.payment_type = 'KARTA' THEN t.amount ELSE 0 END) AS cardAmount
    FROM transaction t
    WHERE t.company_id = :companyId
      AND (:fromDate IS NULL OR t.created_at >= CAST(:fromDate AS TIMESTAMP))
      AND (:toDate IS NULL OR t.created_at <= CAST(:toDate AS TIMESTAMP))
    GROUP BY t.transaction_type
""", nativeQuery = true)
    List<TransactionSummary> getTransactionSummaryByCompanyWithAll(
            @Param("companyId") Long companyId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

    @Query(value = """
            SELECT 
                t.transaction_type AS transactionType,
                SUM(t.amount) AS amount,
                SUM(CASE WHEN t.payment_type = 'NAQD' THEN t.amount ELSE 0 END) AS cashAmount,
                SUM(CASE WHEN t.payment_type = 'TERMINAL' OR t.payment_type = 'PAYMEE' OR t.payment_type = 'CLICK' OR t.payment_type = 'BANK' THEN t.amount ELSE 0 END) AS cardAmount
            FROM transaction t
            WHERE t.company_id = :companyId
               AND t.created_at BETWEEN :fromDate AND :toDate
            GROUP BY t.transaction_type
            """, nativeQuery = true)
    List<TransactionSummary> getTransactionSummaryByCompany(
            @Param("companyId") Long companyId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );


    @Query(value = """
            SELECT 
                t.transaction_type AS transactionType,
                SUM(t.amount) AS amount,
                SUM(CASE WHEN t.payment_type = 'NAQD' THEN t.amount ELSE 0 END) AS cashAmount,
                SUM(CASE WHEN t.payment_type = 'TERMINAL' OR t.payment_type = 'PAYMEE' OR t.payment_type = 'CLICK' OR t.payment_type = 'BANK' THEN t.amount ELSE 0 END) AS cardAmount
            FROM transaction t
            WHERE t.company_id = :companyId
               AND (t.created_at <= :toDate)
            GROUP BY t.transaction_type
            """, nativeQuery = true)
    List<TransactionSummary> getTransactionSummaryByCompanyTo(
            @Param("companyId") Long companyId,
            @Param("toDate") LocalDateTime toDate
    );

    @Query(value = """
            SELECT 
                t.transaction_type AS transactionType,
                SUM(t.amount) AS amount,
                SUM(CASE WHEN t.payment_type = 'NAQD' THEN t.amount ELSE 0 END) AS cashAmount,
                SUM(CASE WHEN t.payment_type = 'TERMINAL' OR t.payment_type = 'PAYMEE' OR t.payment_type = 'CLICK' OR t.payment_type = 'BANK' THEN t.amount ELSE 0 END) AS cardAmount
            FROM transaction t
            WHERE t.company_id = :companyId
               AND (t.created_at >= :fromDate)
            GROUP BY t.transaction_type
            """, nativeQuery = true)
    List<TransactionSummary> getTransactionSummaryByCompanyWithDate(
            @Param("companyId") Long companyId,
            @Param("fromDate") LocalDateTime fromDate
    );

    @Query(value = """
            SELECT 
                t.transaction_type AS transactionType,
                SUM(t.amount) AS amount,
                SUM(CASE WHEN t.payment_type = 'NAQD' THEN t.amount ELSE 0 END) AS cashAmount,
                SUM(CASE WHEN t.payment_type = 'TERMINAL' OR t.payment_type = 'PAYMEE' OR t.payment_type = 'CLICK' OR t.payment_type = 'BANK' THEN t.amount ELSE 0 END) AS cardAmount
            FROM transaction t
            WHERE t.company_id = :companyId
            GROUP BY t.transaction_type
            """, nativeQuery = true)
    List<TransactionSummary> getTransactionSummaryByCompanyWithId(@Param("companyId") Long companyId);

}
