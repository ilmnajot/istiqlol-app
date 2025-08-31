package org.example.moliyaapp.filter;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.criteria.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.example.moliyaapp.entity.MonthlyFee;
import org.example.moliyaapp.entity.Reminder;
import org.example.moliyaapp.entity.StudentContract;
import org.example.moliyaapp.entity.StudentTariff;
import org.example.moliyaapp.enums.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@NoArgsConstructor
@AllArgsConstructor
public class StudentReminderFilter implements Specification<Reminder> {


    private String studentName;
    private Months months;
    private Boolean isReminded;
    private LocalDate estimatedPaymentTime;
    private String comment;

    private Gender gender;
    private StudentGrade grade;
    private Boolean status;
    private LocalDate from; // Start date
    private LocalDate to;   // End date
    private Boolean deleted;
    private Grade stGrade;
    private String academicYear;


    public void setStudentName(String studentName) {
        if (studentName != null) {
            this.studentName = studentName;
        }
    }

    public void setReminded(Boolean reminded) {
        if (reminded != null) {
            isReminded = reminded;
        }
    }

    public void setEstimatedPaymentTime(LocalDate estimatedPaymentTime) {
        if (estimatedPaymentTime != null) {
            this.estimatedPaymentTime = estimatedPaymentTime;
        }
    }

    public void setComment(String comment) {
        if (comment != null) {
            this.comment = comment;
        }
    }

    public void setFrom(LocalDate from) {
        if (from != null) {
            this.from = from;
        }
    }

    public void setTo(LocalDate to) {
        if (to != null) {
            this.to = to;
        }
    }

    public void setStGrade(Grade stGrade) {
        if (stGrade != null) {
            this.stGrade = stGrade;
        }
    }


    public void setGender(Gender gender) {
        if (gender != null) {
            this.gender = gender;
        }
    }

    public void setGrade(StudentGrade grade) {
        if (grade != null) {
            this.grade = grade;
        }
    }

    public void setStatus(Boolean status) {
        log.info("Setting status: {}", status);
        if (status != null) {
            this.status = status;
        }
    }

    public void setMonths(Months months) {
        if (months != null) {
            this.months = months;
        }
    }

    public void setDeleted(Boolean deleted) {
        if (deleted != null) {
            this.deleted = deleted;
        }
    }

    public void setAcademicYear(String academicYear) {
        if (academicYear != null) {
            this.academicYear = academicYear;
        }
    }

    @Override
    public Predicate toPredicate(@NonNull Root<Reminder> root,
                                 CriteriaQuery<?> query,
                                 @NonNull CriteriaBuilder cb) {

        List<Predicate> predicates = new ArrayList<>();

        if (isReminded != null) {
            predicates.add(cb.equal(root.get("isReminded"), isReminded));
        }

        Join<StudentContract, Reminder> reminderJoin = root.join("studentContract", JoinType.INNER);

        if (studentName != null) {
            predicates.add(cb.equal(reminderJoin.get("studentFullName"), studentName));
        }
        if (grade != null) {
            predicates.add(cb.equal(reminderJoin.get("grade"), grade));
        }
        if (gender != null) {
            predicates.add(cb.equal(reminderJoin.get("gender"), gender));
        }
        if (status != null) {
            predicates.add(cb.equal(reminderJoin.get("status"), status));
        }
        if (stGrade != null) {
            predicates.add(cb.equal(reminderJoin.get("stGrade"), stGrade));
        }
        if (academicYear != null) {
            predicates.add(cb.equal(reminderJoin.get("academicYear"), academicYear));
        }
        if (months != null) {
            predicates.add(cb.equal(root.get("month"), months));
        }
        if (estimatedPaymentTime != null) {
            LocalDateTime startOfDay = estimatedPaymentTime.atStartOfDay();
            LocalDateTime endOfDay = estimatedPaymentTime.atTime(LocalTime.MAX);
            predicates.add(cb.between(root.get("estimatedTime"), startOfDay, endOfDay));
        }
        if (comment != null) {
            predicates.add(cb.equal(root.get("comment"), comment));
        }

        if (academicYear != null) {
            predicates.add(cb.equal(reminderJoin.get("academicYear"), academicYear));
        }
        if (from != null || to != null) {
            if (from != null && to != null) {
                predicates.add(cb.between(root.get("createdAt"), from, to));
            } else if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            } else {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }
        }
        if (deleted != null) {
            predicates.add(cb.equal(root.get("deleted"), deleted));
        }

        query.orderBy(cb.desc(root.get("id")));
        query.distinct(true);
        return cb.and(predicates.toArray(new Predicate[0]));
    }

}
