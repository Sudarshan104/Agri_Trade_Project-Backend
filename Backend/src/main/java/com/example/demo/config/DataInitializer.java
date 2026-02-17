package com.example.demo.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import jakarta.annotation.PostConstruct;

import com.example.demo.entity.User;
import com.example.demo.entity.Product;
import com.example.demo.entity.Order;
import com.example.demo.entity.OrderStatus;
import com.example.demo.enums.Role;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.OrderRepository;

@Configuration
public class DataInitializer {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @PostConstruct
    public void createAdminIfNotExists() {

        String adminEmail = "admin@gmail.com";

        if (userRepository.findByEmail(adminEmail).isEmpty()) {

            User admin = new User();
            admin.setName("Admin");
            admin.setEmail(adminEmail);
            admin.setPassword(encoder.encode("admin123")); // 🔐 BCrypt
            admin.setRole(Role.ADMIN);
            admin.setStatus("ACTIVE");

            userRepository.save(admin);

            System.out.println("✅ Admin user created successfully");
        }

        // Create sample farmers
        createSampleFarmers();

        // Create sample retailers
        createSampleRetailers();
    }

    private void createSampleFarmers() {
        if (userRepository.findByEmail("farmer1@gmail.com").isEmpty()) {
            User farmer1 = new User();
            farmer1.setName("John Farmer");
            farmer1.setEmail("farmer1@gmail.com");
            farmer1.setPassword(encoder.encode("farmer123"));
            farmer1.setRole(Role.FARMER);
            farmer1.setStatus("ACTIVE");
            farmer1.setAddress("Farm Address 1");
            farmer1.setVerificationStatus(com.example.demo.enums.VerificationStatus.VERIFIED);
            userRepository.save(farmer1);
            System.out.println("✅ Sample farmer 1 created");
        }

        if (userRepository.findByEmail("farmer2@gmail.com").isEmpty()) {
            User farmer2 = new User();
            farmer2.setName("Jane Farmer");
            farmer2.setEmail("farmer2@gmail.com");
            farmer2.setPassword(encoder.encode("farmer123"));
            farmer2.setRole(Role.FARMER);
            farmer2.setStatus("ACTIVE");
            farmer2.setAddress("Farm Address 2");
            farmer2.setVerificationStatus(com.example.demo.enums.VerificationStatus.VERIFIED);
            userRepository.save(farmer2);
            System.out.println("✅ Sample farmer 2 created");
        }
    }

    private void createSampleRetailers() {
        if (userRepository.findByEmail("retailer1@gmail.com").isEmpty()) {
            User retailer1 = new User();
            retailer1.setName("Bob Retailer");
            retailer1.setEmail("retailer1@gmail.com");
            retailer1.setPassword(encoder.encode("retailer123"));
            retailer1.setRole(Role.RETAILER);
            retailer1.setStatus("ACTIVE");
            retailer1.setAddress("Retail Address 1");
            userRepository.save(retailer1);
            System.out.println("✅ Sample retailer 1 created");
        }

        if (userRepository.findByEmail("retailer2@gmail.com").isEmpty()) {
            User retailer2 = new User();
            retailer2.setName("Alice Retailer");
            retailer2.setEmail("retailer2@gmail.com");
            retailer2.setPassword(encoder.encode("retailer123"));
            retailer2.setRole(Role.RETAILER);
            retailer2.setStatus("ACTIVE");
            retailer2.setAddress("Retail Address 2");
            userRepository.save(retailer2);
            System.out.println("✅ Sample retailer 2 created");
        }
    }


}
