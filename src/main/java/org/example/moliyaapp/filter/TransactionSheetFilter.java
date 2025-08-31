package org.example.moliyaapp.filter;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.example.moliyaapp.entity.Transaction;
import org.example.moliyaapp.enums.PaymentType;
import org.example.moliyaapp.enums.TransactionType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
public class TransactionSheetFilter implements Specification<Transaction> {

    private TransactionType transactionType;
    private PaymentType paymentType;
    private LocalDateTime createdAtFrom;
    private LocalDateTime createdAtTo;
    private Boolean deleted;

    public void setTransactionType(TransactionType transactionType) {
        if (transactionType != null) {
            this.transactionType = transactionType;
        }
    }

    public void setStartDate(LocalDateTime startDate) {
        if (startDate != null) {
            this.createdAtFrom = startDate;
        }
    }

    public void setEndDate(LocalDateTime endDate) {
        if (endDate != null) {
            this.createdAtTo = endDate;
        }
    }

    public void setDeleted(Boolean deleted) {
        if (deleted != null) {
            this.deleted = deleted;
        }
    }

    public void setPaymentType(PaymentType paymentType) {
        if (paymentType != null) {
            this.paymentType = paymentType;
        }
    }

    @Override
    public Predicate toPredicate(Root<Transaction> root,
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

        if (paymentType != null) {
            predicates.add(cb.equal(root.get("paymentType"), paymentType));
        }

        if (deleted != null) {
            predicates.add(cb.equal(root.get("deleted"), deleted));
        }
        query.orderBy(cb.desc(root.get("id")));
        return cb.and(predicates.toArray(new Predicate[0]));
    }
}
