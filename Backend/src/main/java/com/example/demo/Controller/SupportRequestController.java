package com.example.demo.Controller;

import com.example.demo.entity.SupportRequest;
import com.example.demo.services.SupportRequestService;
import com.example.demo.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/support")
@CrossOrigin(origins = "http://localhost:3000")
public class SupportRequestController {

    @Autowired
    private SupportRequestService supportRequestService;

    @Autowired
    private JwtUtil jwtUtil;

    // Create support request (Farmers and Retailers)
    @PostMapping("/request")
    public SupportRequest createSupportRequest(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, String> requestData) {

        String jwtToken = token.replace("Bearer ", "");
        Long userId = jwtUtil.extractUserId(jwtToken);

        String subject = requestData.get("subject");
        String message = requestData.get("message");

        return supportRequestService.createSupportRequest(userId, subject, message);
    }

    // Get user's support requests
    @GetMapping("/my-requests")
    public List<SupportRequest> getMySupportRequests(@RequestHeader("Authorization") String token) {
        String jwtToken = token.replace("Bearer ", "");
        Long userId = jwtUtil.extractUserId(jwtToken);

        return supportRequestService.getSupportRequestsByUser(userId);
    }
}
