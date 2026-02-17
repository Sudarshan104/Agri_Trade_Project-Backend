package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // ================= FARMER =================
    // Get products added by a specific farmer
    List<Product> findByFarmerId(Long farmerId);

    long countByFarmerId(Long farmerId);

    // ================= CATEGORY SEARCH =================
    // Get products by category (Retailer/Admin filter)
    List<Product> findByCategory(String category);

    // ================= NAME SEARCH =================
    List<Product> findByNameContainingIgnoreCase(String name);
}
