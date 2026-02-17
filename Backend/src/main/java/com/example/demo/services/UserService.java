package com.example.demo.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    // ✅ Single encoder instance
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    // ================= REGISTER =================
    public User register(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already in use");
        }

        // 🔐 Encode password before saving
        user.setPassword(encoder.encode(user.getPassword()));

        User savedUser = userRepository.save(user);

        // Send welcome email after successful registration
        try {
            emailService.sendWelcomeEmail(savedUser.getEmail(), savedUser.getName(), savedUser.getRole().toString());
        } catch (Exception e) {
            // Log error but don't fail registration
            System.err.println("Failed to send welcome email: " + e.getMessage());
        }

        return savedUser;
    }

    // ================= LOGIN =================
    public User login(String email, String password) {

        // 1️⃣ Check email exists
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        // 2️⃣ Check password
        if (!encoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        // 3️⃣ Return user (with role)
        return user;
    }

    // ================= RESET PASSWORD =================
    public void resetPassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setPassword(encoder.encode(newPassword));
        userRepository.save(user);
    }
}
