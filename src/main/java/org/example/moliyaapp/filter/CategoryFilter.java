package org.example.moliyaapp.filter;

import jakarta.persistence.criteria.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.example.moliyaapp.entity.Category;
import org.example.moliyaapp.entity.User;
import org.example.moliyaapp.entity.UserRole;
import org.example.moliyaapp.enums.Role;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
public class CategoryFilter implements Specification<Category> {
    private String keyword;
    private Role role;
    private Boolean deleted;

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

    public void setRole(Role role) {
        if (role != null) {
            this.role = role;
        }
    }

    @Override
    public Predicate toPredicate(Root<Category> root,
                                 CriteriaQuery<?> query,
                                 CriteriaBuilder cb) {
        List<Predicate> predicates = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = "%" + keyword.trim().toLowerCase() + "%";
            Predicate keywordPredicate = cb.or(
                    cb.like(cb.lower(root.get("fullName")), kw),
                    cb.like(cb.lower(root.get("phoneNumber")), kw),
                    cb.like(cb.lower(root.get("contractNumber")), kw)
            );
            predicates.add(keywordPredicate);
        }
        Join<User, UserRole> roleJoin = root.join("role");

        if (role != null) {
            predicates.add(cb.equal(roleJoin.get("name"), role.name()));
        }
//        predicates.add(cb.isFalse(root.get("deleted"))); // deleted = false
        predicates.add(cb.equal(root.get("deleted"), deleted));
//        predicates.add(cb.equal(root.get("status"), UserStatus.ACTIVE)); // status = ACTIVE

        query.orderBy(cb.desc(root.get("id")));
        return cb.and(predicates.toArray(new Predicate[0]));
    }
}
