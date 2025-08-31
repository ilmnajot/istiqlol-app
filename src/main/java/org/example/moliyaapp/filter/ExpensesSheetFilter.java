package org.example.moliyaapp.filter;

import jakarta.persistence.criteria.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.example.moliyaapp.entity.Category;
import org.example.moliyaapp.entity.Expenses;
import org.example.moliyaapp.enums.TransactionType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
public class ExpensesSheetFilter implements Specification<Expenses> {
    private TransactionType transactionType;
    private LocalDateTime createdAtFrom;
    private LocalDateTime createdAtTo;
    private Boolean deleted;

    public void setTransactionType(TransactionType transactionType) {
        if (transactionType != null) {
            this.transactionType = transactionType;
        }
    }

    public void setStartDate(LocalDateTime createdAtFrom) {
        if (createdAtFrom != null) {
            this.createdAtFrom =createdAtFrom;
        }
    }

    public void setEndDate(LocalDateTime createdAtTo) {
        if (createdAtTo != null) {
            this.createdAtTo = createdAtTo;
        }
    }

    public void setDeleted(Boolean deleted) {
        if (deleted != null) {
            this.deleted = deleted;
        }
    }

    @Override
    public Predicate toPredicate(Root<Expenses> root,
                                 CriteriaQuery<?> query,
                                 CriteriaBuilder cb) {
        List<Predicate> predicates = new ArrayList<>();



        if (transactionType != null) {
            predicates.add(cb.equal(root.get("transactionType"), transactionType));
        }

        if (createdAtFrom != null || createdAtTo != null) {
            if (createdAtFrom != null && createdAtTo != null) {
                predicates.add(cb.between(root.get("createdAt"), createdAtFrom, createdAtTo));
            } else if (createdAtFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), createdAtFrom));
            } else {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), createdAtTo));
            }
        }

        if (deleted != null) {
            predicates.add(cb.equal(root.get("deleted"), deleted));
        }
        query.orderBy(cb.desc(root.get("id")));
        return cb.and(predicates.toArray(new Predicate[0]));
    }
}
