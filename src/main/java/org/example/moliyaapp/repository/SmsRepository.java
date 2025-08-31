package org.example.moliyaapp.repository;

import org.example.moliyaapp.entity.Sms;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SmsRepository extends JpaRepository<Sms,Long> {
}
