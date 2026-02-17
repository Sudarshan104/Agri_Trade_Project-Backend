package com.example.demo.repository;

import com.example.demo.entity.SupportRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupportRequestRepository extends JpaRepository<SupportRequest, Long> {
    List<SupportRequest> findByUserId(Long userId);
    List<SupportRequest> findAllByOrderByCreatedAtDesc();
}
