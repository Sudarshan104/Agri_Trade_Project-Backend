package com.example.demo.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.example.demo.entity.Order;
import com.example.demo.enums.Role;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.UserRepository;

@RestController
@RequestMapping("/api/delivery-agents")
@CrossOrigin(origins = "http://localhost:3000")
public class DeliveryAgentController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    // 🔹 Get Assigned Orders
    @GetMapping("/{agentId}/orders")
    public List<Order> getAssignedOrders(@PathVariable Long agentId) {
        System.out.println("DEBUG: Fetching assigned orders for Agent ID: " + agentId);
        List<Order> assigned = orderRepository.findByDeliveryAgentId(agentId);
        System.out.println("DEBUG: Found " + assigned.size() + " matches for Agent " + agentId);

        // Optional: extra debug to see what was found
        for (Order o : assigned) {
            System.out.println(
                    "DEBUG: Found Order #" + o.getId() + " for Agent " + agentId + " with status " + o.getStatus());
        }

        return assigned;
    }

    // 🔹 Update Availability (Status)
    // Note: Assuming 'status' field in User entity is used for availability
    // (ACTIVE/INACTIVE)
    @PutMapping("/{agentId}/status")
    public void updateStatus(@PathVariable Long agentId, @RequestBody java.util.Map<String, String> payload) {
        com.example.demo.entity.User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Agent not found"));

        if (agent.getRole() != Role.DELIVERY_AGENT) {
            throw new RuntimeException("User is not a delivery agent");
        }

        agent.setStatus(payload.get("status")); // e.g. "AVAILABLE", "BUSY", "OFFLINE"
        userRepository.save(agent);
    }

    // 🔹 Update Live Location (GPS)
    @PutMapping("/{agentId}/location")
    public void updateLocation(@PathVariable Long agentId, @RequestBody java.util.Map<String, Double> coords) {
        com.example.demo.entity.User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Agent not found"));

        if (coords.containsKey("latitude")) {
            agent.setLatitude(coords.get("latitude"));
        }
        if (coords.containsKey("longitude")) {
            agent.setLongitude(coords.get("longitude"));
        }
        userRepository.save(agent);
    }

    // 🔹 Get Live Location
    @GetMapping("/{agentId}/location")
    public java.util.Map<String, Double> getLocation(@PathVariable Long agentId) {
        com.example.demo.entity.User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Agent not found"));

        if (agent.getLatitude() == null || agent.getLongitude() == null) {
            return java.util.Map.of("latitude", 0.0, "longitude", 0.0);
        }
        return java.util.Map.of("latitude", agent.getLatitude(), "longitude", agent.getLongitude());
    }
}
