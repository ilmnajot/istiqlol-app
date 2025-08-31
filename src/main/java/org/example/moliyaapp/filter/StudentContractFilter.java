package org.example.moliyaapp.filter;

import jakarta.persistence.criteria.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.moliyaapp.entity.MonthlyFee;
import org.example.moliyaapp.entity.StudentContract;
import org.example.moliyaapp.entity.StudentTariff;
import org.example.moliyaapp.enums.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@NoArgsConstructor
@AllArgsConstructor
public class StudentContractFilter implements Specification<StudentContract> {

    private String keyword;
    private Gender gender;
    private StudentGrade grade;
    private StudyLanguage language;
    private GuardianType guardianType;
    private Boolean status;
    private ClientType clientType;
    private LocalDate createdAtFrom;
    private LocalDate createdAtTo;
    private Boolean deleted;
    private Grade stGrade;
    private String tariffName;
    private String academicYear;
    private TariffStatus tariffStatus;
    private PaymentStatus paymentStatus;
    private Months months;
    private Boolean isTransactions;

    public void setKeyword(String keyword) {
        if (keyword != null) {
            this.keyword = keyword;
        }
    }

    public Months getMonths() {
        if (months != null) {
        return months;
        }
        return null;
    }

    public void setStGrade(Grade stGrade) {
        if (stGrade != null) {
            this.stGrade = stGrade;
        }
    }

    public void setTariffName(String tariffName) {
        if (tariffName != null) {
            this.tariffName = tariffName;
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

    public void setLanguage(StudyLanguage language) {
        if (language != null) {
            this.language = language;
        }
    }

    public void setGuardianType(GuardianType guardianType) {
        if (guardianType != null) {
            this.guardianType = guardianType;
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

    public void setStatus(Boolean status) {
        log.info("Setting status: {}", status);
        if (status != null) {
            this.status = status;
        }
    }

    public void setClientType(ClientType clientType) {
        if (clientType != null) {
            this.clientType = clientType;
        }
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        if (paymentStatus != null) {
            this.paymentStatus = paymentStatus;
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

    public void setTariffStatus(TariffStatus tariffStatus) {
        if (tariffStatus != null) {
            this.tariffStatus = tariffStatus;
        }
    }

    public void setIsTransactions(Boolean isTransactions) {
        if (isTransactions != null) {
            this.isTransactions = isTransactions;
        }
    }

    public Boolean getTransactions() {
        return isTransactions;
    }

    @Override
    public Predicate toPredicate(@NonNull Root<StudentContract> root,
                                 CriteriaQuery<?> query,
                                 @NonNull CriteriaBuilder cb) {

        List<Predicate> predicates = new ArrayList<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = "%" + keyword.trim().toLowerCase() + "%";
            Predicate keywordPredicate = cb.or(
                    cb.like(cb.lower(root.get("studentFullName")), kw),
                    cb.like(cb.lower(root.get("guardianFullName")), kw),
                    cb.like(cb.lower(cb.toString(root.get("id"))), kw),
                    cb.like(cb.lower(root.get("phone1")), kw),
                    cb.like(cb.lower(root.get("phone2")), kw)
            );
            predicates.add(keywordPredicate);
        }

        if (gender != null) {
            predicates.add(cb.equal(root.get("gender"), gender));
        }
        if (stGrade != null) {
            predicates.add(cb.equal(root.get("stGrade"), stGrade));
        }
        if (grade != null) {
            predicates.add(cb.equal(root.get("grade"), grade));
        }
        if (language != null) {
            predicates.add(cb.equal(root.get("language"), language));
        }
        if (guardianType != null) {
            predicates.add(cb.equal(root.get("guardianType"), guardianType));
        }
        if (status != null) {
            predicates.add(cb.equal(root.get("status"), status));
        }
        if (clientType != null) {
            predicates.add(cb.equal(root.get("clientType"), clientType));
        }
        if (academicYear != null) {
            predicates.add(cb.equal(root.get("academicYear"), academicYear));
        }

        if (createdAtFrom != null || createdAtTo != null) {
            if (createdAtFrom != null && createdAtTo != null) {
                LocalDateTime startOfDay = createdAtFrom.atStartOfDay();
                LocalDateTime endOfDay = createdAtTo.atTime(23, 59, 59);
                predicates.add(cb.between(root.get("createdAt"), startOfDay, endOfDay));
            } else if (createdAtFrom != null) {
                LocalDateTime startOfDay = createdAtFrom.atStartOfDay();
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startOfDay));
            } else {
                LocalDateTime endOfDay = createdAtTo.atTime(23, 59, 59);
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endOfDay));
            }
        }

        if (deleted != null) {
            predicates.add(cb.equal(root.get("deleted"), deleted));
        }
        if ((tariffName != null && !tariffName.trim().isEmpty()) || tariffStatus != null) {
            Join<StudentContract, StudentTariff> tariffJoin = root.join("tariff", JoinType.LEFT);
            if (tariffName != null && !tariffName.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(tariffJoin.get("name")), "%" + tariffName.toLowerCase() + "%"));
            }
            if (tariffStatus != null) {
                predicates.add(cb.equal(tariffJoin.get("tariffStatus"), tariffStatus));
            }
        }

        // Apply payment status and months filter only if isTransactions is false
        if ((isTransactions == null || !isTransactions) && (paymentStatus != null || months != null)) {
            Join<StudentContract, MonthlyFee> monthlyFeeJoin = root.join("monthlyFees", JoinType.LEFT);
            List<Predicate> monthlyFeePredicates = new ArrayList<>();
            if (months != null) {
                monthlyFeePredicates.add(cb.equal(monthlyFeeJoin.get("months"), months));
            }
            if (paymentStatus != null) {
                if (paymentStatus == PaymentStatus.UNPAID) {
                    Subquery<Long> monthlyFeeExists = query.subquery(Long.class);
                    Root<MonthlyFee> monthlyFeeSubRoot = monthlyFeeExists.from(MonthlyFee.class);
                    monthlyFeeExists.select(cb.count(monthlyFeeSubRoot));
                    List<Predicate> subPredicates = new ArrayList<>();
                    subPredicates.add(cb.equal(monthlyFeeSubRoot.get("studentContract"), root));
                    if (months != null) {
                        subPredicates.add(cb.equal(monthlyFeeSubRoot.get("months"), months));
                    }
                    monthlyFeeExists.where(cb.and(subPredicates.toArray(new Predicate[0])));
                    Predicate unpaidPredicate = cb.or(
                            cb.equal(monthlyFeeExists, 0L),
                            cb.and(
                                    monthlyFeePredicates.isEmpty() ? cb.isTrue(cb.literal(true)) : cb.and(monthlyFeePredicates.toArray(new Predicate[0])),
                                    cb.equal(monthlyFeeJoin.get("status"), PaymentStatus.UNPAID)
                            )
                    );
                    predicates.add(unpaidPredicate);
                } else {
                    monthlyFeePredicates.add(cb.equal(monthlyFeeJoin.get("status"), paymentStatus));
                    predicates.add(cb.and(monthlyFeePredicates.toArray(new Predicate[0])));
                }
            }

            else if (!monthlyFeePredicates.isEmpty()) {
                predicates.add(cb.and(monthlyFeePredicates.toArray(new Predicate[0])));
            }
        }

        query.orderBy(cb.desc(root.get("id")));
        query.distinct(true);
        return cb.and(predicates.toArray(new Predicate[0]));
    }

}
