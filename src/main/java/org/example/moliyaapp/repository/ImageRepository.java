package org.example.moliyaapp.repository;

import org.example.moliyaapp.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {
    Optional<Image> findByUrl(String imageUrl);

    List<Image> findAllByMonthlyFeeId(Long monthlyFeeId);
}
