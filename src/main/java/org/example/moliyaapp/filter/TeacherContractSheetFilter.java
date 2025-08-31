package org.example.moliyaapp.filter;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.example.moliyaapp.entity.Expenses;
import org.example.moliyaapp.entity.TeacherContract;
import org.example.moliyaapp.enums.SalaryType;
import org.example.moliyaapp.enums.TransactionType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
public class TeacherContractSheetFilter implements Specification<TeacherContract> {

    private LocalDateTime createdAtFrom; // Start date
    private LocalDateTime createdAtTo;
    private Boolean deleted;


    public void setCreatedAtFrom(LocalDateTime createdAtFrom) {
        if (createdAtFrom != null) {
            this.createdAtFrom = createdAtFrom;
        }
    }

    public void setCreatedAtTo(LocalDateTime createdAtTo) {
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
    public Predicate toPredicate(Root<TeacherContract> root,
                                 CriteriaQuery<?> query,
                                 CriteriaBuilder cb) {
        List<Predicate> predicates = new ArrayList<>();

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
