package org.example.moliyaapp.filter;

import jakarta.persistence.criteria.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.example.moliyaapp.entity.Category;
import org.example.moliyaapp.entity.Expenses;
import org.example.moliyaapp.entity.User;
import org.example.moliyaapp.entity.UserRole;
import org.example.moliyaapp.enums.Role;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
public class ExpensesFilter implements Specification<Expenses> {
    private String keyword;
    private Boolean deleted;
    private Long categoryId;

    public void setDeleted(Boolean deleted) {
        if (deleted != null) {
            this.deleted = deleted;
        }
    }

    public void setKeyword(String keyword) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            this.keyword = keyword;
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

        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = "%" + keyword.trim().toLowerCase() + "%";
            Predicate keywordPredicate = cb.or(
                    cb.like(cb.lower(root.get("receiver")), kw),
                    cb.like(cb.lower(root.get("spender")), kw),
                    cb.like(cb.lower(root.get("name")), kw)
            );
            predicates.add(keywordPredicate);
        }
        if (deleted != null) {
            predicates.add(cb.equal(root.get("deleted"), deleted));
        }

        if (categoryId != null) {
            Join<Expenses, Category> categoryJoin = root.join("category", JoinType.LEFT);
            predicates.add(cb.equal(categoryJoin.get("id"), categoryId));
        }
        query.orderBy(cb.desc(root.get("id")));
        return cb.and(predicates.toArray(new Predicate[0]));
    }
}
