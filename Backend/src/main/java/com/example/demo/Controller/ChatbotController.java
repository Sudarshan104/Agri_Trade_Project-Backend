package com.example.demo.Controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.services.ChatbotService;

@RestController
@RequestMapping("/api/chatbot")
@CrossOrigin(origins = "http://localhost:3000") // Allow Frontend access
public class ChatbotController {

    @Autowired
    private ChatbotService chatbotService;

    @PostMapping("/query")
    public ResponseEntity<Map<String, String>> chat(@RequestBody Map<String, Object> payload) {
        try {
            String message = (String) payload.get("message");
            Object userIdObj = payload.get("userId");

            if (message == null || message.trim().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("response", "Please enter a message.");
                return ResponseEntity.badRequest().body(error);
            }

            if (userIdObj == null) {
                Map<String, String> error = new HashMap<>();
                error.put("response", "User ID is missing.");
                return ResponseEntity.badRequest().body(error);
            }

            Long userId = Long.valueOf(userIdObj.toString());

            String response = chatbotService.processQuery(message, userId);

            Map<String, String> result = new HashMap<>();
            result.put("response", response);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            // Log error mechanism
            System.err.println("Chatbot Error: " + e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("response", "An error occurred while processing your request.");
            return ResponseEntity.internalServerError().body(error);
        }
    }
}
