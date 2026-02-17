package com.example.demo.Controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.example.demo.entity.Order;
import com.example.demo.entity.User;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.UserRepository;

@RestController
@RequestMapping("/api/debug")
@CrossOrigin(origins = "http://localhost:3000")
public class DebugController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @GetMapping("/dump")
    public Map<String, Object> dumpData() {
        List<User> users = userRepository.findAll();
        List<Order> orders = orderRepository.findAll();

        java.util.List<Map<String, Object>> userDTOs = new java.util.ArrayList<>();
        for (User u : users) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", u.getId());
            map.put("name", u.getName());
            map.put("email", u.getEmail());
            map.put("role", u.getRole() != null ? u.getRole().toString() : "NULL");
            userDTOs.add(map);
        }

        java.util.List<Map<String, Object>> orderDTOs = new java.util.ArrayList<>();
        for (Order o : orders) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", o.getId());
            map.put("status", o.getStatus() != null ? o.getStatus().toString() : "NULL");
            if (o.getDeliveryAgent() != null) {
                map.put("deliveryAgentId", o.getDeliveryAgent().getId());
                map.put("deliveryAgentName", o.getDeliveryAgent().getName());
            } else {
                map.put("deliveryAgentId", "NULL");
                map.put("deliveryAgentName", "NULL");
            }
            map.put("pickupOtp", o.getPickupOtp() != null ? o.getPickupOtp() : "NULL");
            map.put("deliveryOtp", o.getDeliveryOtp() != null ? o.getDeliveryOtp() : "NULL");
            orderDTOs.add(map);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("users", userDTOs);
        response.put("orders", orderDTOs);
        return response;
    }

    @GetMapping("/fix/{orderId}/{agentId}")
    public String fixAssignment(@PathVariable Long orderId, @PathVariable Long agentId) {
        try {
            Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));
            User agent = userRepository.findById(agentId).orElseThrow(() -> new RuntimeException("Agent not found"));

            order.setDeliveryAgent(agent);

            // Force status to SHIPPED if PLACED/PACKED to ensure visibility
            if (order.getStatus() != null &&
                    (order.getStatus() == com.example.demo.entity.OrderStatus.PLACED ||
                            order.getStatus() == com.example.demo.entity.OrderStatus.PACKED)) {
                order.setStatus(com.example.demo.entity.OrderStatus.SHIPPED);
            }

            orderRepository.save(order);
            return "SUCCESS: Assigned Order " + orderId + " to Agent " + agentId + " (" + agent.getName() + ")";
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    @GetMapping("/force-sql/{orderId}/{agentId}")
    @org.springframework.transaction.annotation.Transactional
    public String forceSqlFix(@PathVariable Long orderId, @PathVariable Long agentId) {
        try {
            int updated = entityManager
                    .createNativeQuery(
                            "UPDATE orders SET delivery_agent_id = :agentId, status = 'SHIPPED' WHERE id = :orderId")
                    .setParameter("agentId", agentId)
                    .setParameter("orderId", orderId)
                    .executeUpdate();

            return "SQL SUCCESS: Updated " + updated + " rows. Assigned Order " + orderId + " to Agent " + agentId;
        } catch (Exception e) {
            e.printStackTrace();
            return "SQL ERROR: " + e.getMessage();
        }
    }
}
