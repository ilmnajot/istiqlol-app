package org.example.moliyaapp.filter;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.example.moliyaapp.entity.StudentContract;
import org.example.moliyaapp.enums.Grade;
import org.example.moliyaapp.enums.StudentGrade;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
public class StudentContractByGenderFilter implements Specification<StudentContract> {
    private String academicYear;
    private StudentGrade studentGrade;
    private Grade stGrade;

    public void setAcademicYear(String academicYear) {
        if (academicYear != null) {
            this.academicYear = academicYear;
        }
    }

    public void setStudentGrade(StudentGrade studentGrade) {
        if (studentGrade != null) {
            this.studentGrade = studentGrade;
        }
    }

    public void setStGrade(Grade stGrade) {
        if (stGrade != null) {
            this.stGrade = stGrade;
        }
    }

    @Override
    public Predicate toPredicate(Root<StudentContract> root,
                                 CriteriaQuery<?> query,
                                 CriteriaBuilder cb) {

        List<Predicate> predicates = new ArrayList<>();
        if (academicYear != null) {
            predicates.add(cb.equal(root.get("academicYear"), academicYear));
        }
        if (studentGrade != null) {
            predicates.add(cb.equal(root.get("studentGrade"), stGrade));
        }
        if (stGrade != null) {
            predicates.add(cb.equal(root.get("stGrade"), stGrade));
        }


        query.orderBy(cb.desc(root.get("id")));
        query.distinct(true);
        return cb.and(predicates.toArray(new Predicate[0]));

    }
}
