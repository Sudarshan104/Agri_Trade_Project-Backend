package com.example.demo.services;

import com.example.demo.entity.SupportRequest;
import com.example.demo.entity.User;
import com.example.demo.enums.SupportStatus;
import com.example.demo.repository.SupportRequestRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SupportRequestService {

    @Autowired
    private SupportRequestRepository supportRequestRepository;

    @Autowired
    private UserRepository userRepository;

    public SupportRequest createSupportRequest(Long userId, String subject, String message) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        SupportRequest request = new SupportRequest();
        request.setUser(user);
        request.setSubject(subject);
        request.setMessage(message);
        request.setStatus(SupportStatus.PENDING);
        request.setCreatedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());

        return supportRequestRepository.save(request);
    }

    public List<SupportRequest> getAllSupportRequests() {
        return supportRequestRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<SupportRequest> getSupportRequestsByUser(Long userId) {
        return supportRequestRepository.findByUserId(userId);
    }

    public SupportRequest updateSupportRequestStatus(Long requestId, SupportStatus status, String adminResponse) {
        SupportRequest request = supportRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Support request not found"));

        request.setStatus(status);
        if (adminResponse != null) {
            request.setAdminResponse(adminResponse);
        }
        request.setUpdatedAt(LocalDateTime.now());

        return supportRequestRepository.save(request);
    }
}
