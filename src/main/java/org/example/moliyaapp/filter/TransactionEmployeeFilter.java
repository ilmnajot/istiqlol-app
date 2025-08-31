package org.example.moliyaapp.filter;

import jakarta.persistence.criteria.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.moliyaapp.entity.MonthlyFee;
import org.example.moliyaapp.entity.StudentContract;
import org.example.moliyaapp.entity.Transaction;
import org.example.moliyaapp.entity.User;
import org.example.moliyaapp.enums.Months;
import org.example.moliyaapp.enums.PaymentType;
import org.example.moliyaapp.enums.TransactionType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@AllArgsConstructor
@NoArgsConstructor
public class TransactionEmployeeFilter implements Specification<Transaction> {
    private String employeeName;
    private String keyword;
    private String amountType; // "MAIN" for main salary, "BONUS" for bonus
    private TransactionType type;
    private Months month;
    private Boolean deleted;
    private PaymentType paymentType;
    private LocalDate from;
    private LocalDate to;

    public void setKeyword(String keyword) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            this.keyword = keyword.trim();
            log.info("Keyword set to: {}", this.keyword);
        } else {
            this.keyword = null;
            log.info("Keyword set to null (input was null or empty)");
        }
    }
    public void setFrom(LocalDate from) {
        if (from != null) {
            this.from = from;
            log.info("From date set to: {}", this.from);
        }
    }
    public void setTo(LocalDate to) {
        if (to != null) {
            this.to = to;
            log.info("To date set to: {}", this.to);
        }
    }
    public void setPaymentType(PaymentType paymentType) {
        if (paymentType!=null){
            this.paymentType=paymentType;
        }
    }

    public void setEmployeeName(String employeeName) {
        if (employeeName != null && !employeeName.trim().isEmpty()) {
            this.employeeName = employeeName.trim();
            log.info("EmployeeName set to: {}", this.employeeName);
        } else {
            this.employeeName = null;
        }
    }

    public void setAmountType(String amountType) {
        if (amountType != null && !amountType.trim().isEmpty()) {
            this.amountType = amountType.trim().toUpperCase();
            log.info("AmountType set to: {}", this.amountType);
        } else {
            this.amountType = null;
            log.info("AmountType set to null");
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

    public void setDeleted(Boolean deleted) {
        if (deleted != null) {
            this.deleted = deleted;
            log.info("Deleted set to: {}", deleted);
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

        // Join with MonthlyFee if needed
        if (month != null || keyword != null || type != null || employeeName != null) {
            monthlyFeeJoin = root.join("monthlyFee", JoinType.INNER);
            log.info("Joined Transaction to MonthlyFee (INNER)");

            if (month != null) {
                predicates.add(cb.equal(monthlyFeeJoin.get("months"), month));
                log.info("Added month predicate: {}", month);
            }

            // Employee name filtering with LIKE for partial matches
            if (employeeName != null) {
                employeeJoin = monthlyFeeJoin.join("employee", JoinType.INNER);
                String searchPattern = "%" + employeeName.toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(employeeJoin.get("fullName")), searchPattern));
                log.info("Added employee name predicate: {}", employeeName);
            }

            // Transaction type filtering
            if (type != null) {
                predicates.add(cb.equal(root.get("transactionType"), type));

                if (type == TransactionType.INCOME) {
                    // For student transactions, ensure studentContract exists
                    studentContractJoin = monthlyFeeJoin.join("studentContract", JoinType.INNER);
                    log.info("Applied student-related filter: monthlyFee.studentContract != null");
                }
                log.info("Applied transaction type filter: {}", type);
            }
        }

        // Amount type filtering based on description
        if (amountType != null) {
            switch (amountType) {
                case "MAIN":
                    predicates.add(cb.like(root.get("description"), "ASOSIY_TO'LOV_TRANSAKSIYASI:%"));
                    log.info("Applied amountType filter for MAIN salary");
                    break;
                case "BONUS":
                    predicates.add(cb.like(root.get("description"), "BONUS_TO'LOV_TRANSAKSIYASI:%"));
                    log.info("Applied amountType filter for BONUS");
                    break;
                default:
                    log.warn("Unknown amountType: {}. Valid values are MAIN or BONUS", amountType);
                    break;
            }
        }
        // Payment type filtering
        if (paymentType != null) {
            predicates.add(cb.equal(root.get("paymentType"), paymentType));
            log.info("Applied payment type filter: {}", paymentType);
        }

        // Keyword search
        if (keyword != null) {
            String searchPattern = "%" + keyword.toLowerCase() + "%";
            List<Predicate> keywordPredicates = new ArrayList<>();

            if (monthlyFeeJoin != null) {
                // Initialize joins if not already done
                if (studentContractJoin == null) {
                    studentContractJoin = monthlyFeeJoin.join("studentContract", JoinType.LEFT);
                }
                if (employeeJoin == null) {
                    employeeJoin = monthlyFeeJoin.join("employee", JoinType.LEFT);
                }

                // Search studentFullName and employee fullName
                keywordPredicates.add(cb.like(cb.lower(cb.coalesce(studentContractJoin.get("studentFullName"), "")), searchPattern));
                keywordPredicates.add(cb.like(cb.lower(cb.coalesce(employeeJoin.get("fullName"), "")), searchPattern));
                log.info("Added student and employee name search predicates");
            }

            // Search in transaction description
            keywordPredicates.add(cb.like(cb.lower(root.get("description")), searchPattern));
            log.info("Added transaction description search predicate");

            predicates.add(cb.or(keywordPredicates.toArray(new Predicate[0])));
            log.info("Applied keyword search with pattern: {}", searchPattern);
        }

        if (from != null || to != null) {
            if (from != null && to != null) {
                predicates.add(cb.between(root.get("createdAt"), from, to));
            } else if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            } else {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }
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