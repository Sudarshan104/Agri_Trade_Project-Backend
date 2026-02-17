package com.example.demo.Controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.example.demo.entity.Order;
import com.example.demo.entity.Product;
import com.example.demo.entity.User;
import com.example.demo.entity.SupportRequest;
import com.example.demo.entity.IssueReport;
import com.example.demo.enums.VerificationStatus;
import com.example.demo.enums.SupportStatus;
import com.example.demo.enums.IssueStatus;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.SupportRequestRepository;
import com.example.demo.repository.IssueReportRepository;
import com.example.demo.services.UserService;
import com.example.demo.services.SupportRequestService;
import com.example.demo.services.IssueReportService;
import java.util.HashMap;

import jakarta.transaction.Transactional;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:3000")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserService userService; // Inject userService

    @Autowired
    private SupportRequestRepository supportRequestRepository;

    @Autowired
    private IssueReportRepository issueReportRepository;

    @Autowired
    private SupportRequestService supportRequestService;

    @Autowired
    private IssueReportService issueReportService;

    @Autowired
    private com.example.demo.services.EmailService emailService;

    @Autowired
    private com.example.demo.services.NotificationService notificationService;

    // ================= USERS =================

    // 🔹 Get all users (Farmers + Retailers)
    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // 🔹 Update user details (Admin use)
    @PutMapping("/users/{id}")
    public User updateUser(@PathVariable Long id, @RequestBody User u) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setName(u.getName());
        user.setEmail(u.getEmail());
        user.setRole(u.getRole());

        return userRepository.save(user);
    }

    // 🔹 Update user status (ACTIVE / INACTIVE / BLOCKED)
    @PutMapping("/users/{id}/status")
    public User updateUserStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setStatus(body.get("status"));
        return userRepository.save(user);
    }

    // 🔹 Delete user
    @DeleteMapping("/users/{id}")
    public void deleteUser(@PathVariable Long id) {
        userRepository.deleteById(id);
    }

    // 🔹 Reset User Password (to 123456)
    @PutMapping("/users/{id}/reset-password")
    public void resetUserPassword(@PathVariable Long id) {
        if (userService != null)
            userService.resetPassword(id, "123456");
    }

    // 🔹 Create Delivery Agent
    @PostMapping("/delivery-agents")
    public User createDeliveryAgent(@RequestBody User user) {
        user.setRole(com.example.demo.enums.Role.DELIVERY_AGENT);
        // Default verification status
        user.setVerificationStatus(VerificationStatus.VERIFIED);

        // ✅ Set Default Password if missing
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            user.setPassword("123456");
        }

        if (userService != null)
            return userService.register(user);

        return userRepository.save(user);
    }

    // ================= PROFILE (ADMIN / FARMER / RETAILER) =================

    // 🔹 Get profile by ID
    @GetMapping("/profile/{id}")
    public User getProfile(@PathVariable Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // 🔹 Update profile
    @PutMapping("/profile/{id}")
    public User updateProfile(@PathVariable Long id, @RequestBody User u) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setName(u.getName());
        user.setAddress(u.getAddress());

        return userRepository.save(user);
    }

    // ================= PRODUCTS =================

    // 🔹 Get all products (Admin view)
    @GetMapping("/products")
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    // 🔹 Update product
    @PutMapping("/products/{id}")
    public Product updateProduct(@PathVariable Long id, @RequestBody Product p) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        product.setName(p.getName());
        product.setPrice(p.getPrice());
        product.setQuantity(p.getQuantity());

        return productRepository.save(product);
    }

    // 🔹 Delete product
    @DeleteMapping("/products/{id}")
    public void deleteProduct(@PathVariable Long id) {
        productRepository.deleteById(id);
    }

    // ================= ORDERS =================

    // 🔹 Get all orders
    @GetMapping("/orders")
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    // 🔹 Assign delivery agent to order
    // 🔹 Assign delivery agent to order
    @PutMapping("/orders/{orderId}/assign-delivery-agent")
    @Transactional
    public org.springframework.http.ResponseEntity<?> assignDeliveryAgent(
            @PathVariable Long orderId,
            @RequestBody Map<String, Object> data) { // Changed to Object for safer casting
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found"));

            Object agentIdObj = data.get("deliveryAgentId");
            if (agentIdObj == null) {
                return org.springframework.http.ResponseEntity.badRequest().body("Delivery agent ID is required");
            }
            Long deliveryAgentId = Long.valueOf(agentIdObj.toString());

            User deliveryAgent = userRepository.findById(deliveryAgentId)
                    .orElseThrow(() -> new RuntimeException("Delivery agent not found"));

            if (deliveryAgent.getRole() != com.example.demo.enums.Role.DELIVERY_AGENT) {
                return org.springframework.http.ResponseEntity.badRequest().body("User is not a delivery agent");
            }

            // Only allow assignment for orders that are packed or shipped
            // Relaxed check: Allow PLACED orders to be assigned too if business logic
            // permits,
            // OR return clear error if not.
            // User error was "Internal Server Error", likely due to this check failing on
            // PLACED order.
            // We will keep the check but return 400 with CLEAR message.
            if (order.getStatus() != com.example.demo.entity.OrderStatus.PACKED) {
                return org.springframework.http.ResponseEntity.badRequest()
                        .body("Order must be PACKED to assign delivery agent. Current status: " + order.getStatus());
            }

            // If strictly PACKED/SHIPPED is required, remove PLACED from above check.
            // Assuming user tried on PLACED order.

            System.out.println("DEBUG: Assigning Agent ID " + deliveryAgentId + " to Order ID " + orderId);
            order.setDeliveryAgent(deliveryAgent);

            // ✅ Generate Pickup OTP and notify Farmer
            String pickupOtp = String.format("%06d", new java.util.Random().nextInt(999999));
            order.setPickupOtp(pickupOtp);

            orderRepository.saveAndFlush(order);
            System.out.println("DEBUG: Saved Order " + orderId + ". Agent is now: " + order.getDeliveryAgent().getId());

            // 🔹 Notify Farmer about assigned agent and Pickup OTP
            if (order.getProduct() != null && order.getProduct().getFarmer() != null) {
                try {
                    String farmerMsg = "Agent assigned for Order #" + order.getId() + ". Your Pickup OTP is: "
                            + pickupOtp;
                    notificationService.createNotification(order.getProduct().getFarmer().getId(), farmerMsg);

                    emailService.sendPickupOtpNotification(
                            order.getProduct().getFarmer().getEmail(),
                            order.getProduct().getFarmer().getName(),
                            String.valueOf(order.getId()),
                            pickupOtp);
                } catch (Exception e) {
                    System.err.println("Failed to send pickup OTP notifications: " + e.getMessage());
                }
            }

            // 🔹 Notify Delivery Agent about new task
            if (deliveryAgent.getEmail() != null) {
                try {
                    String agentMsg = "New task: Order #" + order.getId() + " assigned to you.";
                    notificationService.createNotification(deliveryAgent.getId(), agentMsg);

                    emailService.sendOrderStatusUpdateNotification(
                            deliveryAgent.getEmail(),
                            deliveryAgent.getName(),
                            String.valueOf(order.getId()),
                            "ASSIGNED_TO_YOU",
                            "You have been assigned a new delivery task. Please check your dashboard for details.");
                } catch (Exception e) {
                    System.err.println("Failed to send task notifications to agent: " + e.getMessage());
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Delivery agent assigned successfully!");
            response.put("pickupOtp", pickupOtp);
            response.put("orderId", orderId);

            return org.springframework.http.ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return org.springframework.http.ResponseEntity.status(500).body("Error assigning agent: " + e.getMessage());
        }
    }

    // ================= DOCUMENT VERIFICATION =================

    // 🔹 Get pending verifications
    @GetMapping("/pending-verifications")
    public List<User> getPendingVerifications() {
        return userRepository.findAll().stream()
                .filter(user -> user.getVerificationStatus() == VerificationStatus.PENDING)
                .toList();
    }

    // 🔹 Verify user (Approve/Reject)
    @PutMapping("/verify-user/{userId}")
    @Transactional
    public Map<String, String> verifyUser(
            @PathVariable Long userId,
            @RequestBody Map<String, Object> data) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean approved = Boolean.parseBoolean(data.get("approved").toString());

        if (approved) {
            user.setVerificationStatus(VerificationStatus.VERIFIED);
            return Map.of("message", "User verified successfully.");
        } else {
            String reason = data.get("reason").toString();
            user.setVerificationStatus(VerificationStatus.REJECTED);
            user.setRejectionReason(reason);
            return Map.of("message", "User rejected. Reason: " + reason);
        }
    }

    // ================= SUPPORT REQUESTS =================

    // 🔹 Get all support requests
    @GetMapping("/support-requests")
    public List<SupportRequest> getAllSupportRequests() {
        return supportRequestRepository.findAllByOrderByCreatedAtDesc();
    }

    // 🔹 Update support request status and response
    @PutMapping("/support-requests/{id}")
    public SupportRequest updateSupportRequest(
            @PathVariable Long id,
            @RequestBody Map<String, Object> data) {

        SupportRequest request = supportRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Support request not found"));

        if (data.containsKey("status")) {
            request.setStatus(SupportStatus.valueOf(data.get("status").toString()));
        }

        if (data.containsKey("adminResponse")) {
            request.setAdminResponse(data.get("adminResponse").toString());
            request.setUpdatedAt(java.time.LocalDateTime.now());
        }

        return supportRequestRepository.save(request);
    }

    // ================= ISSUE REPORTS =================

    // 🔹 Get all issue reports
    @GetMapping("/issue-reports")
    public List<IssueReport> getAllIssueReports() {
        return issueReportRepository.findAllByOrderByCreatedAtDesc();
    }

    // 🔹 Update issue report status and response
    @PutMapping("/issue-reports/{id}")
    public IssueReport updateIssueReport(
            @PathVariable Long id,
            @RequestBody Map<String, Object> data) {

        IssueReport report = issueReportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Issue report not found"));

        if (data.containsKey("status")) {
            report.setStatus(IssueStatus.valueOf(data.get("status").toString()));
        }

        if (data.containsKey("adminResponse")) {
            report.setAdminResponse(data.get("adminResponse").toString());
            report.setUpdatedAt(java.time.LocalDateTime.now());
        }

        return issueReportRepository.save(report);
    }
}
