package com.example.demo.Controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.example.demo.entity.User;
import com.example.demo.enums.Role;
import com.example.demo.services.UserService;
import com.example.demo.utils.JwtUtil;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private com.example.demo.services.EmailService emailService;

    // REGISTER
    @PostMapping("/register")
    public User register(@RequestBody User user) {

        // 🔐 Security: prevent ADMIN registration from fronted
        if (user.getRole() == Role.ADMIN || user.getRole() == null) {
            user.setRole(Role.FARMER);
        }

        User registeredUser = userService.register(user);

        // 🔹 Send Welcome Email
        if (registeredUser.getRole() == Role.FARMER || registeredUser.getRole() == Role.RETAILER
                || registeredUser.getRole() == Role.DELIVERY_AGENT) {
            try {
                emailService.sendWelcomeEmail(
                        registeredUser.getEmail(),
                        registeredUser.getName(),
                        registeredUser.getRole().toString());
            } catch (Exception e) {
                System.err.println("Failed to send welcome email: " + e.getMessage());
            }
        }

        return registeredUser;
    }

    // LOGIN
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> data) {
        User user = userService.login(
                data.get("email"),
                data.get("password"));

        // Generate JWT token
        String token = jwtUtil.generateToken(user.getEmail(), user.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("user", user);
        response.put("token", token);

        return response;
    }
}
