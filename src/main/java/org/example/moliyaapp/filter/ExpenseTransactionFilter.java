package org.example.moliyaapp.filter;

import jakarta.persistence.criteria.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.moliyaapp.entity.*;
import org.example.moliyaapp.enums.Months;
import org.example.moliyaapp.enums.PaymentType;
import org.example.moliyaapp.enums.TransactionType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@AllArgsConstructor
@NoArgsConstructor
public class ExpenseTransactionFilter implements Specification<Transaction> {

    private String keyword;
    private PaymentType paymentType;
    private TransactionType type;
    private LocalDate from;
    private LocalDate to;
    private Boolean deleted;
    private String categoryName; // New field for expense category filtering

    public void setKeyword(String keyword) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            this.keyword = keyword.trim();
            log.info("Keyword set to: {}", this.keyword);
        } else {
            this.keyword = null;
            log.info("Keyword set to null (input was null or empty)");
        }
    }

    public void setType(TransactionType type) {
        if (type != null) {
            this.type = type;
            log.info("TransactionType set to: {}", type);
        }
    }

    public void setFrom(LocalDate from) {
        if (from != null) {
            this.from = from;
            log.info("From date set to: {}", from);
        }
    }

    public void setTo(LocalDate to) {
        if (to != null) {
            this.to = to;
            log.info("To date set to: {}", to);
        }
    }

    public void setDeleted(Boolean deleted) {
        if (deleted != null) {
            this.deleted = deleted;
            log.info("Deleted set to: {}", deleted);
        }
    }

    public void setPaymentType(PaymentType paymentType) {
        if (paymentType != null) {
            this.paymentType = paymentType;
            log.info("PaymentType set to: {}", paymentType);
        }
    }

    public void setCategoryName(String categoryName) {
        if (categoryName != null && !categoryName.trim().isEmpty()) {
            this.categoryName = categoryName.trim();
            log.info("CategoryName set to: {}", categoryName);
        }
    }

    @Override
    public Predicate toPredicate(Root<Transaction> root,
                                 CriteriaQuery<?> query,
                                 CriteriaBuilder cb) {
        List<Predicate> predicates = new ArrayList<>();

        // Ensure distinct results to avoid duplicates
        query.distinct(true);

        // Include only transactions where expense ID is not null
        predicates.add(cb.isNotNull(root.get("expenses")));
        log.info("Including only transactions where expense ID is not null");

        // Joins
        Join<Transaction, Expenses> expensesJoin = null;
        Join<Expenses, Category> categoryJoin = null;

        // Always join with expenses since we're filtering for expense transactions
        expensesJoin = root.join("expenses", JoinType.INNER);
        log.info("Joined Transaction to Expenses (INNER)");

        // Join with category if category filtering is needed
        if (categoryName != null) {
            categoryJoin = expensesJoin.join("category", JoinType.INNER);
            log.info("Joined Expenses to Category (INNER)");
        }

        // Type filtering - now based on expense transaction type
        if (type != null) {
            predicates.add(cb.equal(expensesJoin.get("transactionType"), type));
            log.info("Applied expense transaction type filter: {}", type);
        }

        // Category name filtering
        if (categoryName != null && categoryJoin != null) {
            String searchPattern = "%" + categoryName.toLowerCase() + "%";
            predicates.add(cb.like(cb.lower(cb.coalesce(categoryJoin.get("name"), "")), searchPattern));
            log.info("Applied category name filter with pattern: {}", searchPattern);
        }

        // Keyword search
        if (keyword != null) {
            String searchPattern = "%" + keyword.toLowerCase() + "%";
            List<Predicate> keywordPredicates = new ArrayList<>();

            // Search in expense fields
            keywordPredicates.add(cb.like(cb.lower(cb.coalesce(expensesJoin.get("name"), "")), searchPattern));
            keywordPredicates.add(cb.like(cb.lower(cb.coalesce(expensesJoin.get("receiver"), "")), searchPattern));
            keywordPredicates.add(cb.like(cb.lower(cb.coalesce(expensesJoin.get("spender"), "")), searchPattern));
            keywordPredicates.add(cb.like(cb.lower(cb.coalesce(expensesJoin.get("description"), "")), searchPattern));

            // Search in transaction description
            keywordPredicates.add(cb.like(cb.lower(cb.coalesce(root.get("description"), "")), searchPattern));

            // Also search in category name if category join exists
            if (categoryJoin != null) {
                keywordPredicates.add(cb.like(cb.lower(cb.coalesce(categoryJoin.get("name"), "")), searchPattern));
            }

            predicates.add(cb.or(keywordPredicates.toArray(new Predicate[0])));
            log.info("Applied keyword search with pattern: {}", searchPattern);
        }

        // Date range filtering
        if (this.from != null || this.to != null) {
            if (this.from != null && this.to != null) {
                LocalDateTime start = this.from.atStartOfDay();
                LocalDateTime end = this.to.atTime(LocalTime.MAX);
                predicates.add(cb.between(root.get("createdAt"), start, end));
                log.info("Applied date range filter: {} to {}", start, end);
            } else if (this.from != null) {
                LocalDateTime start = this.from.atStartOfDay();
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), start));
                log.info("Applied date from filter: {}", start);
            } else {
                LocalDateTime end = this.to.atTime(LocalTime.MAX);
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), end));
                log.info("Applied date to filter: {}", end);
            }
        }

        // Payment type filtering - check both transaction and expense payment types
        if (paymentType != null) {
            List<Predicate> paymentTypePredicates = new ArrayList<>();
            paymentTypePredicates.add(cb.equal(root.get("paymentType"), paymentType));
            paymentTypePredicates.add(cb.equal(expensesJoin.get("paymentType"), paymentType));
            predicates.add(cb.or(paymentTypePredicates.toArray(new Predicate[0])));
            log.info("Applied payment type filter: {}", paymentType);
        }

        // Deleted status filtering
        if (deleted != null) {
            predicates.add(cb.equal(root.get("deleted"), deleted));
            log.info("Applied deleted filter: {}", deleted);
        }

        // Log total predicates
        log.info("Total predicates applied: {}", predicates.size());

        // Order by id descending
        query.orderBy(cb.desc(root.get("id")));
        log.info("Applied order by id descending");
        return cb.and(predicates.toArray(new Predicate[0]));
    }
}