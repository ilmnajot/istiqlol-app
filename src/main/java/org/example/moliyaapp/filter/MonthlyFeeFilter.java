package org.example.moliyaapp.filter;

import jakarta.persistence.criteria.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.example.moliyaapp.entity.Category;
import org.example.moliyaapp.entity.Expenses;
import org.example.moliyaapp.entity.MonthlyFee;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
public class MonthlyFeeFilter implements Specification<MonthlyFee> {
    private Boolean deleted;
    private String categoryName;

    public void setDeleted(Boolean deleted) {
        if (deleted != null) {
            this.deleted = deleted;
        }
    }
    public void setCategoryName(String categoryName) {
        if (categoryName != null) {
            this.categoryName = categoryName;
        }
    }



    @Override
    public Predicate toPredicate(Root<MonthlyFee> root,
                                 CriteriaQuery<?> query,
                                 CriteriaBuilder cb) {
        List<Predicate> predicates = new ArrayList<>();

        if (deleted != null) {
            predicates.add(cb.equal(root.get("deleted"), deleted));
        }

        if (categoryName != null) {
            Join<Expenses, Category> categoryJoin = root.join("category", JoinType.LEFT);
            predicates.add(cb.equal(categoryJoin.get("name"), categoryJoin));
        }
        query.orderBy(cb.desc(root.get("id")));
        return cb.and(predicates.toArray(new Predicate[0]));
    }
}
