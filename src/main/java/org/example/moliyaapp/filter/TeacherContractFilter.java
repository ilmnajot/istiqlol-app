package org.example.moliyaapp.filter;

import jakarta.persistence.criteria.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.example.moliyaapp.entity.TeacherContract;
import org.example.moliyaapp.entity.User;
import org.example.moliyaapp.entity.UserRole;
import org.example.moliyaapp.enums.Role;
import org.example.moliyaapp.enums.SalaryType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
public class TeacherContractFilter implements Specification<TeacherContract> {
    Logger log = LoggerFactory.getLogger(TeacherContract.class);
    private SalaryType salaryType;
    private String keyword;
    private Role role;
    private Integer lessonsCountsFrom;
    private Integer lessonsCountsTo;
    private LocalDate createdAtFrom; // Start date
    private LocalDate createdAtTo;
    private Boolean deleted;
    private Boolean active;


    public void setRole(Role role) {
        if (role != null) {
            this.role = role;
        }
    }

    public void setLessonsCountsTo(Integer lessonsCountsTo) {
        if (lessonsCountsTo != null && lessonsCountsTo > 0) {
            this.lessonsCountsTo = lessonsCountsTo;
        }
    }

    public void setKeyword(String keyword) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            this.keyword = keyword;
        }
    }

    public void setLessonsCountsFrom(Integer lessonsCountsFrom) {
        if (lessonsCountsFrom != null && lessonsCountsFrom > 0) {
            this.lessonsCountsFrom = lessonsCountsFrom;
        }
    }

    public void setSalaryType(SalaryType salaryType) {
        if (salaryType != null) {
            this.salaryType = salaryType;
        }
    }

    public void setDeleted(Boolean deleted) {
        if (deleted != null) {
            this.deleted = deleted;
        }
    }

    public void setCreatedAtFrom(LocalDate createdAtFrom) {
        if (createdAtFrom != null) {
            this.createdAtFrom = createdAtFrom;
        }
    }

    public void setCreatedAtTo(LocalDate createdAtTo) {
        if (createdAtTo != null) {
            this.createdAtTo = createdAtTo;
        }
    }

    public void setActive(Boolean active) {
        if (active != null) {
            this.active = active;
        }
    }

    @Override
    public Predicate toPredicate(Root<TeacherContract> root,
                                 CriteriaQuery<?> query,
                                 CriteriaBuilder cb) {
        List<Predicate> predicates = new ArrayList<>();

        if (salaryType != null) {
            predicates.add(cb.equal(root.get("salaryType"), salaryType));
        }
        if (lessonsCountsFrom != null) {
            predicates.add(cb.equal(root.get("lessonCountPerMonth"), lessonsCountsFrom));
        }
        if (lessonsCountsTo != null && lessonsCountsTo > 0) {
            predicates.add(cb.equal(root.get("lessonCountPerMonth"), lessonsCountsTo));
        }
        if (createdAtFrom != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), createdAtFrom));
        }
        if (createdAtTo != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), createdAtTo));
        }
//        predicates.add(cb.isFalse(root.get("deleted"))); // deleted = false

        if (deleted != null) {
            predicates.add(cb.equal(root.get("deleted"), deleted));
        }
        if (active != null) {
            predicates.add(cb.equal(root.get("active"), active));
        }

        // Join TeacherContract -> User (teacher)
        Join<TeacherContract, User> teacherJoin = root.join("teacher");

        // Join User -> Role (roles)
        Join<User, UserRole> roleJoin = teacherJoin.join("role");

        if (role != null) {
            predicates.add(cb.equal(roleJoin.get("name"), role.name()));
        }

        if (keyword != null && !keyword.trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(teacherJoin.get("fullName")), "%" + keyword.toLowerCase() + "%"));
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

        query.orderBy(cb.desc(root.get("id")));
        return cb.and(predicates.toArray(new Predicate[0]));
    }


}
