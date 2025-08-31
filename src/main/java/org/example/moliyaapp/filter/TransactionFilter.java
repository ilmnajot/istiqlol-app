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
public class TransactionFilter implements Specification<Transaction> {

    private String keyword;
    private PaymentType paymentType;
    private TransactionType type;
    private Months month;
    private LocalDate from;
    private LocalDate to;
    private Boolean deleted;
    private String academicYear;

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

    public void setMonth(Months month) {
        if (month != null) {
            this.month = month;
            log.info("Month set to: {}", month);
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

    public void setAcademicYear(String academicYear) {
        if (academicYear != null) {
            this.academicYear = academicYear.trim();
            log.info("AcademicYear set to: {}", academicYear);
        }
    }

    @Override
    public Predicate toPredicate(Root<Transaction> root,
                                 CriteriaQuery<?> query,
                                 CriteriaBuilder cb) {
        List<Predicate> predicates = new ArrayList<>();

        // Ensure distinct results to avoid duplicates
        query.distinct(true);

        // Always exclude transactions with expenses
        predicates.add(cb.isNull(root.get("expenses")));
        log.info("Excluding transactions with expenses");

        // Joins
        Join<Transaction, MonthlyFee> monthlyFeeJoin = null;
        Join<MonthlyFee, StudentContract> studentContractJoin = null;
        Join<MonthlyFee, User> employeeJoin = null;

        // Determine if we need MonthlyFee join
        boolean needsMonthlyFeeJoin = month != null || keyword != null || type != null || academicYear != null;

        // Determine if we need StudentContract join
        boolean needsStudentContractJoin = type == TransactionType.INCOME || academicYear != null || keyword != null;

        if (needsMonthlyFeeJoin) {
            monthlyFeeJoin = root.join("monthlyFee", JoinType.INNER);
            log.info("Joined Transaction to MonthlyFee (INNER)");

            if (month != null) {
                predicates.add(cb.equal(monthlyFeeJoin.get("months"), month));
                log.info("Added month predicate: {}", month);
            }
        }

        if (needsStudentContractJoin) {
            if (monthlyFeeJoin == null) {
                monthlyFeeJoin = root.join("monthlyFee", JoinType.INNER);
                log.info("Joined Transaction to MonthlyFee (INNER) for StudentContract");
            }
            // Use INNER JOIN for academic year filtering to ensure we only get student transactions
            studentContractJoin = monthlyFeeJoin.join("studentContract", JoinType.INNER);
            log.info("Joined MonthlyFee to StudentContract (INNER)");
        }

        // Type filtering for student or employee transactions
        if (type != null) {
            if (type == TransactionType.INCOME) {
                // Already ensured studentContractJoin exists above
                predicates.add(cb.equal(root.get("transactionType"), TransactionType.INCOME));
                log.info("Applied student-related filter: transactionType=INCOME");
            } else if (type == TransactionType.OUTCOME) {
                // Employee-related: transactionType=OUTCOME
                predicates.add(cb.equal(root.get("transactionType"), TransactionType.OUTCOME));
                log.info("Applied employee-related filter: transactionType=OUTCOME");
            }
        }

        // Academic year filtering
        if (academicYear != null) {
            // studentContractJoin is already created above with INNER join
            predicates.add(cb.equal(studentContractJoin != null ? studentContractJoin.get("academicYear") : null, academicYear));
            log.info("Applied academic year filter: {}", academicYear);
        }

        // Keyword search
        if (keyword != null) {
            String searchPattern = "%" + keyword.toLowerCase() + "%";
            List<Predicate> keywordPredicates = new ArrayList<>();

            if (monthlyFeeJoin != null) {
                // Initialize employeeJoin if needed
                if (employeeJoin == null) {
                    employeeJoin = monthlyFeeJoin.join("employee", JoinType.LEFT);
                }

                // Search studentFullName and employee fullName
                if (studentContractJoin != null) {
                    keywordPredicates.add(cb.like(cb.lower(cb.coalesce(studentContractJoin.get("studentFullName"), "")), searchPattern));
                }
                keywordPredicates.add(cb.like(cb.lower(cb.coalesce(employeeJoin.get("fullName"), "")), searchPattern));
                log.info("Added student and employee name search predicates");
            }

            // Search in transaction description
            keywordPredicates.add(cb.like(cb.lower(root.get("description")), searchPattern));
            log.info("Added transaction description search predicate");

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

        // Payment type filtering
        if (paymentType != null) {
            predicates.add(cb.equal(root.get("paymentType"), paymentType));
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