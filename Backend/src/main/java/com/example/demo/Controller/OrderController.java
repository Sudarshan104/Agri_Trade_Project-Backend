package com.example.demo.Controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Random;

import com.example.demo.entity.Order;
import com.example.demo.entity.OrderStatus;
import com.example.demo.entity.Product;
import com.example.demo.entity.User;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.services.EmailService;

import jakarta.transaction.Transactional;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "http://localhost:3000")
public class OrderController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private com.example.demo.services.NotificationService notificationService;

    // 🔹 Create Order
    @PostMapping("/create")
    @Transactional
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> payload) {
        try {
            Long retailerId = Long.valueOf(payload.get("retailerId").toString());
            Long productId = Long.valueOf(payload.get("productId").toString());
            int quantity = Integer.parseInt(payload.get("quantity").toString());
            Object paymentIntentIdObj = payload.get("paymentIntentId");
            String paymentIntentId = (paymentIntentIdObj != null) ? paymentIntentIdObj.toString() : null;
            Double totalAmount = payload.containsKey("totalAmount")
                    ? Double.valueOf(payload.get("totalAmount").toString())
                    : 0.0;

            User retailer = userRepository.findById(retailerId)
                    .orElseThrow(() -> new RuntimeException("Retailer not found"));

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            // ✅ Calculate total if 0 (moved here)
            if (totalAmount == 0.0 && product.getPrice() != null) {
                totalAmount = product.getPrice() * quantity;
            }

            if (product.getQuantity() < quantity) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Insufficient stock available");
            }

            // Deduct stock
            product.setQuantity(product.getQuantity() - quantity);
            productRepository.save(product);

            Order order = new Order();
            order.setRetailer(retailer);
            order.setProduct(product);
            order.setQuantity(quantity);
            order.setStatus(OrderStatus.PLACED);
            order.setOrderDate(LocalDateTime.now());
            order.setPaymentIntentId(paymentIntentId);
            order.setPaymentStatus("PAID"); // Assuming paid if intent ID present, adapt as needed
            order.setTotalAmount(totalAmount);

            Order savedOrder = orderRepository.save(order);
            return ResponseEntity.ok(savedOrder);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating order: " + e.getMessage());
        }
    }

    // 🔹 Get Order by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderById(@PathVariable Long id) {
        Optional<Order> order = orderRepository.findById(id);
        if (order.isPresent()) {
            return ResponseEntity.ok(order.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Order not found");
        }
    }

    // 🔹 Get Orders by User (Retailer or Farmer)
    // Used for "My Orders" page
    @GetMapping("/user/{userId}")
    public List<Order> getOrdersByUser(@PathVariable Long userId, @RequestParam String role) {
        if ("RETAILER".equalsIgnoreCase(role)) {
            return orderRepository.findByRetailerId(userId);
        } else if ("FARMER".equalsIgnoreCase(role)) {
            return orderRepository.findByProductFarmerId(userId);
        }
        return List.of();
    }

    // 🔹 Get Assigned Orders for Delivery Agent
    @GetMapping("/agent/{agentId}")
    public List<Order> getOrdersByAgent(@PathVariable Long agentId) {
        return orderRepository.findByDeliveryAgentId(agentId);
    }

    // 🔹 Get All Orders (Admin)
    @GetMapping("/all")
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    // 🔹 Get Farmer Analytics
    @GetMapping("/farmer/{farmerId}/analytics")
    public ResponseEntity<?> getFarmerAnalytics(@PathVariable Long farmerId) {
        try {
            // 1. Total Revenue
            Double totalRevenue = orderRepository.sumRevenueByFarmerId(farmerId, OrderStatus.DELIVERED);

            // 2. Completed Transactions
            List<Order> completedTransactions = orderRepository.findByProductFarmerIdAndStatus(farmerId,
                    OrderStatus.DELIVERED);

            // 3. Monthly Transactions (Trend)
            List<Object[]> monthlyData = orderRepository.getMonthlyTransactionCounts(farmerId, OrderStatus.DELIVERED);
            Map<String, Long> monthlyTransactions = new java.util.LinkedHashMap<>();
            String[] months = { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };
            for (Object[] row : monthlyData) {
                // Safer casting for Number types (Integer, Long, etc.)
                Number monthNum = (Number) row[0];
                int monthIndex = monthNum.intValue() - 1; // SQL month is 1-based
                if (monthIndex >= 0 && monthIndex < 12) {
                    monthlyTransactions.put(months[monthIndex], (Long) row[1]);
                }
            }

            // 4. Top Sold Products
            List<Object[]> topProductsData = orderRepository.getTopSoldProductsTop5(farmerId, OrderStatus.DELIVERED);
            List<Map<String, Object>> topSoldProducts = new java.util.ArrayList<>();
            for (Object[] row : topProductsData) {
                topSoldProducts.add(Map.of("name", row[0], "quantity", row[1]));
            }

            // 5. Monthly Sales (Revenue) - Helper logic if not in repository, or mock for
            // now
            // Detailed revenue by month query not in repository yet, using transaction
            // count as proxy or skipping
            // The frontend map expects 'monthlySales'. Let's reuse monthlyTransactions
            // logic or query specifically.
            // For now, let's return monthlyTransactions as monthlySales proxy or adding
            // simple mock.
            // Ideally, we need 'getMonthlyRevenue'.
            Map<String, Double> monthlySales = new java.util.LinkedHashMap<>(); // Empty for now or impl query

            Map<String, Object> response = new HashMap<>();
            response.put("totalRevenue", totalRevenue != null ? totalRevenue : 0.0);
            response.put("completedTransactions", completedTransactions);
            response.put("monthlyTransactions", monthlyTransactions);
            response.put("topSoldProducts", topSoldProducts);
            response.put("monthlySales", monthlySales); // Frontend expects this

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error fetching farmer analytics: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching analytics: " + e.getMessage());
        }
    }

    // 🔹 Get Retailer Analytics
    @GetMapping("/retailer/{retailerId}/analytics")
    public ResponseEntity<?> getRetailerAnalytics(@PathVariable Long retailerId) {
        System.out.println(">>> Fetching RETAILER Analytics for ID: " + retailerId);
        try {
            Map<String, Object> response = new HashMap<>();

            // 1. Order Status Counts
            long placed = orderRepository.countByRetailerIdAndStatus(retailerId, OrderStatus.PLACED);
            long modified = orderRepository.countByRetailerIdAndStatus(retailerId, OrderStatus.MODIFIED);
            long cancelled = orderRepository.countByRetailerIdAndStatus(retailerId, OrderStatus.CANCELLED);

            long delivered = orderRepository.countByRetailerIdAndStatus(retailerId, OrderStatus.DELIVERED);
            long completed = orderRepository.countByRetailerIdAndStatus(retailerId, OrderStatus.COMPLETED);

            long processing = orderRepository.countByRetailerIdAndStatus(retailerId, OrderStatus.PROCESSING);
            long stockConfirmed = orderRepository.countByRetailerIdAndStatus(retailerId, OrderStatus.STOCK_CONFIRMED);
            long packed = orderRepository.countByRetailerIdAndStatus(retailerId, OrderStatus.PACKED);
            long shipped = orderRepository.countByRetailerIdAndStatus(retailerId, OrderStatus.SHIPPED);
            long outForDelivery = orderRepository.countByRetailerIdAndStatus(retailerId, OrderStatus.OUT_FOR_DELIVERY);

            response.put("placedOrders", placed);
            response.put("modifiedOrders", modified);
            response.put("cancelledOrders", cancelled);
            response.put("deliveredOrders", delivered + completed);

            // Group all intermediate active states into "Processing"
            long totalProcessing = processing + stockConfirmed + packed + shipped + outForDelivery;
            response.put("processingOrders", totalProcessing);

            // 2. Monthly Transactions (Delivered Orders Count Trend)
            List<Object[]> monthlyData = orderRepository.getMonthlyRetailerTransactions(retailerId,
                    OrderStatus.DELIVERED);
            Map<String, Long> monthlyTransactions = new LinkedHashMap<>();
            String[] months = { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };
            for (Object[] row : monthlyData) {
                Number monthNum = (Number) row[0];
                int monthIndex = monthNum.intValue() - 1;
                if (monthIndex >= 0 && monthIndex < 12) {
                    monthlyTransactions.put(months[monthIndex], (Long) row[1]);
                }
            }
            response.put("monthlyTransactions", monthlyTransactions);

            // 3. Top Purchased Products
            List<Object[]> topProductsData = orderRepository.getTopPurchasedProducts(retailerId, OrderStatus.DELIVERED);
            List<Map<String, Object>> topPurchasedProducts = new ArrayList<>();
            int limit = 0;
            for (Object[] row : topProductsData) {
                if (limit++ >= 5)
                    break;
                topPurchasedProducts.add(Map.of("name", row[0], "quantity", row[1]));
            }
            response.put("topPurchasedProducts", topPurchasedProducts);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("Error fetching retailer analytics: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching analytics: " + e.getMessage());
        }
    }

    // 🔹 Resend Delivery OTP
    @PostMapping("/{id}/resend-otp")
    public ResponseEntity<?> resendDeliveryOtp(@PathVariable Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getStatus() != OrderStatus.OUT_FOR_DELIVERY) {
            return ResponseEntity.badRequest().body("OTP can only be resent for orders that are OUT_FOR_DELIVERY.");
        }

        if (order.getDeliveryOtp() == null) {
            // Generate if missing for some reason
            String generatedOtp = String.format("%06d", new java.util.Random().nextInt(999999));
            order.setDeliveryOtp(generatedOtp);
            orderRepository.save(order);
        }

        // 🔹 Send Email Notification
        if (order.getRetailer() != null && order.getRetailer().getEmail() != null) {
            try {
                System.out.println("DEBUG: Resending Delivery OTP to: " + order.getRetailer().getEmail());
                emailService.sendOrderStatusUpdateNotification(
                        order.getRetailer().getEmail(),
                        order.getRetailer().getName(),
                        String.valueOf(order.getId()),
                        order.getStatus().toString(),
                        "Resending your Delivery OTP: " + order.getDeliveryOtp());

                Map<String, String> response = new HashMap<>();
                response.put("message", "Delivery OTP has been resent to " + order.getRetailer().getEmail());
                return ResponseEntity.ok(response);
            } catch (Exception e) {
                System.err.println("Failed to resend order status email: " + e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to send email: " + e.getMessage());
            }
        }
        return ResponseEntity.badRequest().body("Retailer email not found.");
    }

    // 🔹 Update Order Status
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {

        String newStatusStr = payload.get("status");
        String otp = payload.get("otp");
        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(newStatusStr);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid status");
        }

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        System.out.println("DEBUG: Updating status for Order #" + id + " to " + newStatus);

        // 🔐 OTP Logic
        if (newStatus == OrderStatus.OUT_FOR_DELIVERY) {
            // 1️⃣ Validate Pickup OTP (from Farmer)
            if (order.getPickupOtp() == null || !order.getPickupOtp().equals(otp)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Invalid or missing Pickup OTP. Please get the OTP from the Farmer to confirm pickup.");
            }

            // 2️⃣ Generate 6-digit OTP for Delivery (to Retailer)
            String generatedOtp = String.format("%06d", new java.util.Random().nextInt(999999));
            order.setDeliveryOtp(generatedOtp);
            System.out.println("DEBUG: Generated Delivery OTP: " + generatedOtp + " for Order #" + id);
            // We will send this OTP in the email below
        }

        if (newStatus == OrderStatus.DELIVERED) {
            // Validate Delivery OTP (from Retailer)
            if (order.getDeliveryOtp() == null || !order.getDeliveryOtp().equals(otp)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Invalid or missing Delivery OTP. Please get the OTP from the Retailer.");
            }
        }

        order.setStatus(newStatus);

        if (newStatus == OrderStatus.DELIVERED) {
            // Logic for delivery completion if any (e.g. update payment status if COD)
            order.setPaymentStatus("PAID"); // Assume COD or final settlement
        }

        orderRepository.save(order);

        // 🔹 Send Internal Notifications
        if (order.getRetailer() != null) {
            String msg = "Order #" + order.getId() + " status updated to " + newStatus;
            if (newStatus == OrderStatus.OUT_FOR_DELIVERY) {
                msg += ". Your Delivery OTP is: " + order.getDeliveryOtp();
            }
            notificationService.createNotification(order.getRetailer().getId(), msg);
        }

        if (order.getProduct() != null && order.getProduct().getFarmer() != null) {
            String farmerMsg = "Order #" + order.getId() + " (" + order.getProduct().getName() + ") is now "
                    + newStatus;
            notificationService.createNotification(order.getProduct().getFarmer().getId(), farmerMsg);
        }

        // 🔹 Send Email Notification
        if (order.getRetailer() != null && order.getRetailer().getEmail() != null) {
            try {
                String extraMessage = "";
                if (newStatus == OrderStatus.OUT_FOR_DELIVERY) {
                    extraMessage = " Your Delivery OTP is: " + order.getDeliveryOtp();
                }

                System.out.println("DEBUG: Sending status update email to: " + order.getRetailer().getEmail()
                        + " for status: " + newStatus);
                emailService.sendOrderStatusUpdateNotification(
                        order.getRetailer().getEmail(),
                        order.getRetailer().getName(),
                        String.valueOf(order.getId()),
                        newStatus.toString(),
                        "Your order status has been updated. " + extraMessage);
            } catch (Exception e) {
                System.err.println("Failed to send order status email: " + e.getMessage());
                // Don't fail the request if email fails
            }
        }

        return ResponseEntity.ok(Map.of("message", "Order status updated to " + newStatus));
    }
}
