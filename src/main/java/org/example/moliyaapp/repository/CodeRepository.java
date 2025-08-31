package org.example.moliyaapp.repository;

import org.example.moliyaapp.entity.Code;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CodeRepository extends JpaRepository<Code, Long> {
    @Query(value = "select * from codes as c where c.user_id=?1", nativeQuery = true)
    Optional<Code> findByUserId(Long id);
}
