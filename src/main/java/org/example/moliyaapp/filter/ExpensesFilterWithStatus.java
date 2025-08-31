package org.example.moliyaapp.filter;

import jakarta.persistence.criteria.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.example.moliyaapp.entity.Category;
import org.example.moliyaapp.entity.Expenses;
import org.example.moliyaapp.enums.CategoryStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
public class ExpensesFilterWithStatus implements Specification<Expenses> {
    private String keyword;
    private Boolean deleted;
    private CategoryStatus categoryStatus;
    private Long categoryId;
    private LocalDate createdAt;
    private LocalDateTime from;
    private LocalDateTime to;

    public void setDeleted(Boolean deleted) {
        if (deleted != null) {
            this.deleted = deleted;
        }
    }
    public void setKeyword(String keyword) {
        if (keyword != null && !keyword.isBlank()) {
            this.keyword = keyword.trim();
        }
    }

    public void setFrom(LocalDateTime from) {
        if (from != null) {
            this.from = from;
        }
    }

    public void setTo(LocalDateTime to) {
        if (to != null) {
            this.to = to;
        }
    }

    public void setDate(LocalDate createdAt) {
        if (createdAt != null) {
            this.createdAt = createdAt;

        }
    }

    public void setCategoryStatus(CategoryStatus categoryStatus) {
        if (categoryStatus != null) {
            this.categoryStatus = categoryStatus;
        }
    }

    public void setCategoryId(Long categoryId) {
        if (categoryId != null) {
            this.categoryId = categoryId;
        }
    }

    @Override
    public Predicate toPredicate(Root<Expenses> root,
                                 CriteriaQuery<?> query,
                                 CriteriaBuilder cb) {
        List<Predicate> predicates = new ArrayList<>();
        //key word to search by name and description
        if (keyword != null) {
            String likePattern = "%" + keyword.toLowerCase() + "%";
            Predicate namePredicate = cb.like(cb.lower(root.get("name")), likePattern);
            Predicate descriptionPredicate = cb.like(cb.lower(root.get("description")), likePattern);
            predicates.add(cb.or(namePredicate, descriptionPredicate));
        }
//        if (keyword != null && !keyword.trim().isEmpty()) {
//            String kw = "%" + keyword.trim().toLowerCase() + "%";
//            Predicate keywordPredicate = cb.or(
//                    cb.like(cb.lower(root.get("receiver")), kw),
//                    cb.like(cb.lower(root.get("spender")), kw),
//                    cb.like(cb.lower(root.get("name")), kw)
//            );
//            predicates.add(keywordPredicate);
//        }

        if (deleted != null) {
            predicates.add(cb.equal(root.get("deleted"), deleted));
        }

        Join<Expenses, Category> categoryJoin = root.join("category", JoinType.LEFT);
        if (categoryStatus != null) {
            predicates.add(cb.equal(categoryJoin.get("categoryStatus"), categoryStatus));
        }
        if (categoryId != null) {
            predicates.add(cb.equal(categoryJoin.get("id"), categoryId));
        }

        // 🔹 DATE ustuvor, agar mavjud bo‘lsa
        if (createdAt != null) {
            LocalDateTime startOfDay = createdAt.atStartOfDay();      // 00:00:00
            LocalDateTime endOfDay = createdAt.plusDays(1).atStartOfDay(); // ertasi kuni 00:00:00
            predicates.add(cb.between(root.get("createdAt"), startOfDay, endOfDay));
        }
        // 🔹 Agar DATE bo‘lmasa, FROM/TO ishlaydi
        else if (from != null || to != null) {
            if (from != null && to != null) {
                predicates.add(cb.between(root.get("createdAt"), from, to));
            } else if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            } else {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }
        }
        query.orderBy(cb.desc(root.get("id")));
        return cb.and(predicates.toArray(new Predicate[0]));
    }


}
