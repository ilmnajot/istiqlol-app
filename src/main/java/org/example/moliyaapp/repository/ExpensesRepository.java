package org.example.moliyaapp.repository;

import org.example.moliyaapp.entity.Expenses;
import org.example.moliyaapp.enums.CategoryStatus;
import org.example.moliyaapp.enums.Months;
import org.example.moliyaapp.enums.TransactionType;
import org.example.moliyaapp.projection.GetExpenseData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpensesRepository extends JpaRepository<Expenses, Long>, JpaSpecificationExecutor<Expenses> {

    @Query("select e from expenses as e where e.deleted=true")
    Page<Expenses> findAllDeleted(Pageable pageable);

    Optional<Expenses> findByIdAndDeletedFalse(Long id);

    @Query("select coalesce(sum(e.amount), 0) from expenses as e where e.createdAt between :start and :end")
    Double getTotalExpensesBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    Page<Expenses> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    Page<Expenses> findAllByCategoryId(Long categoryId, Pageable pageable);

    @Query("select e.id from expenses as e " +
            "where e.id in :ids " +
            "and e.deleted=true")
    List<Long> findAllByDeletedTrue(@Param("ids") List<Long> ids);

    @Modifying
    @Transactional
    @Query("delete from expenses as e where e.id in :ids")
    void deleteAllByIds(@Param("ids") List<Long> ids);

    @Query("select e.category.name as categoryName, sum(e.amount) as amount, e.paymentType as paymentType " +
            "from expenses as e " +
            "where e.transactionType = :transactionType " +
            "and (cast(:from as timestamp) is null or e.createdAt >= :from) " +
            "and (cast(:to as timestamp) is null or e.createdAt <= :to) " +
            "group by e.paymentType, e.category.name")
    List<GetExpenseData> getExpensesDataByMonth(
            @Param("transactionType") TransactionType transactionType,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}
