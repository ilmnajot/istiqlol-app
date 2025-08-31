package org.example.moliyaapp.filter;

import jakarta.persistence.criteria.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.example.moliyaapp.entity.*;
import org.example.moliyaapp.enums.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
public class MonthlyFeeFilterWithStatus implements Specification<MonthlyFee> {
    private Boolean deleted;
    private String keyword;
    private CategoryStatus categoryStatus;
    private Months months;
    private TariffName tariffName;
    private PaymentStatus status;
    private LocalDate createdAt;

    public void setCreatedAt(LocalDate createdAt) {
        if (createdAt != null) {
            this.createdAt = createdAt;
        }
    }

    public void setDeleted(Boolean deleted) {
        if (deleted != null) {
            this.deleted = deleted;
        }
    }

    public void setCategoryStatus(CategoryStatus categoryStatus) {
        if (categoryStatus != null) {
            this.categoryStatus = categoryStatus;
        }
    }

    public void setMonths(Months months) {
        if (months != null) {
            this.months = months;
        }
    }

    public void setTariffName(TariffName tariffName) {
        if (tariffName != null) {
            this.tariffName = tariffName;
        }
    }

    public void setStatus(PaymentStatus status) {
        if (status != null) {
            this.status = status;
        }
    }

    public void setKeyword(String keyword) {
        if (keyword != null) {
            this.keyword = keyword;

        }
    }

    @Override
    public Predicate toPredicate(Root<MonthlyFee> root,
                                 CriteriaQuery<?> query,
                                 CriteriaBuilder cb) {
        List<Predicate> predicates = new ArrayList<>();
        Join<MonthlyFee, StudentContract> studentContractJoin = root.join("studentContract", JoinType.LEFT);
        Join<MonthlyFee, User> userJoin = root.join("employee", JoinType.LEFT);
        if (keyword != null && !keyword.isEmpty()) {
            String searchPattern = "%" + keyword.toLowerCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(studentContractJoin.get("studentFullName")), searchPattern),
                    cb.like(cb.lower(userJoin.get("fullName")), searchPattern)

            ));
        }
        if (deleted != null) {
            predicates.add(cb.equal(root.get("deleted"), deleted));
        }
        if (months != null) {
            predicates.add(cb.equal(root.get("months"), months.name()));
        }
        if (tariffName != null) {
            predicates.add(cb.equal(root.get("tariffName"), tariffName.name()));
        }
        if (status != null) {
            predicates.add(cb.equal(root.get("status"), status.name()));
        }
        if (createdAt != null) {
            LocalDateTime startOfDay = createdAt.atStartOfDay();               // 2025-06-02T00:00:00
            LocalDateTime endOfDay = createdAt.plusDays(1).atStartOfDay();     // 2025-06-03T00:00:00
            predicates.add(cb.between(root.get("createdAt"), startOfDay, endOfDay));
        }


        if (categoryStatus != null) {
            Join<MonthlyFee, Category> categoryJoin = root.join("category", JoinType.LEFT);
            predicates.add(cb.equal(categoryJoin.get("categoryStatus"), categoryStatus.name()));
        }
        query.orderBy(cb.desc(root.get("id")));
        return cb.and(predicates.toArray(new Predicate[0]));
    }
}
