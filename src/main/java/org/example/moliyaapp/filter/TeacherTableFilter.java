package org.example.moliyaapp.filter;

import jakarta.persistence.criteria.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.example.moliyaapp.entity.TeacherTable;
import org.example.moliyaapp.entity.User;
import org.example.moliyaapp.entity.UserRole;
import org.example.moliyaapp.enums.Months;
import org.example.moliyaapp.enums.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@NoArgsConstructor
@AllArgsConstructor
public class TeacherTableFilter implements Specification<TeacherTable> {
    private static final Logger log = LoggerFactory.getLogger(TeacherTableFilter.class);
    private String keyword;
    private Role role;
    private Months months;
    private Integer year;
    private Boolean otherRoles;
    private Boolean deleted;
    private LocalDate createdAtFrom; // Start date
    private LocalDate createdAtTo;


    // Setters for the filter fields (including year for specific checks)
    public void setYear(Integer year) {
        if (year != null) {
            this.year = year;
        }
    }

    public void setRole(Role role) {
        if (role != null) {
            this.role = role;
        }
    }

    public void setOtherRoles(Boolean otherRoles) {
        if (otherRoles != null) {
            this.otherRoles = otherRoles;
        }
    }

    public void setMonths(Months months) {
        if (months != null) {
            this.months = months;
        }

    }

    public void setKeyword(String keyword) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            this.keyword = keyword;
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

    public void setDeleted(Boolean deleted) {
        if (deleted != null) {
            this.deleted = deleted;
        }
    }

    @Override
    public Predicate toPredicate(Root<TeacherTable> root,
                                 CriteriaQuery<?> query,
                                 CriteriaBuilder cb) {

        List<Predicate> predicates = new ArrayList<>();
        log.info("Filtering with keyword={}, role={}, otherRoles={}, months={}, year={}, deleted={}, createdAtFrom={}, createdAtTo={}",
                keyword, role, otherRoles, months, year, deleted, createdAtFrom, createdAtTo);


        // TeacherTable -> User
        Join<TeacherTable, User> teacherJoin = root.join("teacher", JoinType.LEFT);

        // User -> UserRole (ManyToMany)
        Join<User, UserRole> userRoleJoin = teacherJoin.joinSet("role", JoinType.LEFT);

        Set<String> teacherAndEmployee = Set.of(Role.TEACHER.name(), Role.EMPLOYEE.name());

        // Role filtering
        if (role != null) {
            // If a specific role is provided, filter by that role
            predicates.add(cb.equal(userRoleJoin.get("name"), role.name()));
        } else if (otherRoles != null) {
            // Handle otherRoles when no specific role is provided
            if (otherRoles) { //todo // Boolean.TRUE.equals(otherRoles) => changed to the current check.
                // Exclude TEACHER and EMPLOYEE roles
                predicates.add(cb.or(
                        cb.not(userRoleJoin.get("name").in(teacherAndEmployee)),
                        userRoleJoin.get("name").isNull()
                ));
            } else {
                // Include only TEACHER and EMPLOYEE roles
                predicates.add(userRoleJoin.get("name").in(teacherAndEmployee));
            }
        }
        // When role is null and otherRoles is null, skip role filtering entirely

        if (keyword != null && !keyword.trim().isEmpty()) {
            String pattern = "%" + keyword.toLowerCase() + "%";
            predicates.add(cb.like(cb.lower(teacherJoin.get("fullName")), pattern));
        }
        if (months != null) {
            predicates.add(cb.equal(root.get("months"), months));
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
        if (year != null) {
            Expression<Integer> yearExpression = cb.function(
                    "date_part",
                    Integer.class,
                    cb.literal("year"),
                    root.get("createdAt")
            );
            predicates.add(cb.equal(yearExpression, year));
        }
        if (deleted != null) {
            predicates.add(cb.equal(root.get("deleted"), deleted));
        }

        query.orderBy(cb.desc(root.get("id")));
        return cb.and(predicates.toArray(new Predicate[0]));
    }
//        return cb.conjunction();
//    }
}
