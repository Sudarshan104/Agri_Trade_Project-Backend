package com.example.demo.Controller;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

import com.example.demo.entity.User;
import com.example.demo.enums.VerificationStatus;
import com.example.demo.repository.UserRepository;
import com.example.demo.services.CloudinaryService;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:3000")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CloudinaryService cloudinaryService;

    // ================= GET PROFILE =================
    @GetMapping("/{userId}")
    public User getProfile(@PathVariable Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // ================= UPDATE PROFILE =================
    @PutMapping("/{userId}")
    @Transactional
    public User updateProfile(
            @PathVariable Long userId,
            @RequestBody Map<String, Object> data
    ) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        /* ===== SAFE UPDATES (NO NULL CRASH) ===== */

        if (data.get("name") != null) {
            user.setName(data.get("name").toString());
        }

        // ❌ Do NOT allow email change (recommended)
        // If you REALLY want it, uncomment below
        /*
        if (data.get("email") != null) {
            user.setEmail(data.get("email").toString());
        }
        */

        if (data.get("address") != null) {
            user.setAddress(data.get("address").toString());
        }



        return userRepository.save(user);
    }

    // ================= GET USER COUNT =================
    @GetMapping("/count")
    public long getUserCount() {
        return userRepository.count();
    }

    // ================= UPLOAD DOCUMENTS =================
    @PostMapping(value = "/upload-documents/{userId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public Map<String, String> uploadDocuments(
            @PathVariable Long userId,
            @RequestParam("aadhaarFile") MultipartFile aadhaarFile,
            @RequestParam("panFile") MultipartFile panFile,
            @RequestParam("aadhaarNumber") String aadhaarNumber,
            @RequestParam("panNumber") String panNumber
    ) throws IOException {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Validate file types and sizes
        validateFile(aadhaarFile);
        validateFile(panFile);

        // Upload to Cloudinary
        String aadhaarUrl = cloudinaryService.uploadFile(aadhaarFile);
        String panUrl = cloudinaryService.uploadFile(panFile);

        // Update user
        user.setAadhaarNumber(aadhaarNumber);
        user.setPanNumber(panNumber);
        user.setAadhaarDocUrl(aadhaarUrl);
        user.setPanDocUrl(panUrl);
        user.setVerificationStatus(VerificationStatus.PENDING);

        userRepository.save(user);

        return Map.of("message", "Documents uploaded successfully. Awaiting admin verification.");
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        if (file.getSize() > 5 * 1024 * 1024) { // 5MB limit
            throw new IllegalArgumentException("File size must be less than 5MB");
        }

        String contentType = file.getContentType();
        if (!isValidFileType(contentType)) {
            throw new IllegalArgumentException("Invalid file type. Only JPG, PNG, PDF allowed.");
        }
    }

    private boolean isValidFileType(String contentType) {
        return contentType != null && (
            contentType.equals("image/jpeg") ||
            contentType.equals("image/png") ||
            contentType.equals("application/pdf")
        );
    }
}
