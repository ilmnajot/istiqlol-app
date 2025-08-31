package org.example.moliyaapp.repository;

import org.example.moliyaapp.entity.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReminderRepository extends JpaRepository<Reminder, Long>, JpaSpecificationExecutor<Reminder> {
    List<Reminder> findByStudentContractId(Long id);

    @Query("select r from Reminder as r where r.studentContract.id=:studentContractId order by r.id desc")
    List<Reminder> findAllByStudentContractId(@Param("studentContractId") Long studentContractId);
}
