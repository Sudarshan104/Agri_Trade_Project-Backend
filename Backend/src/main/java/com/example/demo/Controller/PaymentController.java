package com.example.demo.Controller;

import com.example.demo.entity.Order;
import com.example.demo.repository.OrderRepository;
import com.example.demo.services.EmailService;
import com.example.demo.services.PdfService;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "http://localhost:3000")
public class PaymentController {

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    private final OrderRepository orderRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PdfService pdfService;

    public PaymentController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> request) {
        try {
            String orderIdsStr = request.get("orderId").toString();
            Integer amount = (Integer) request.get("amount");
            String currency = (String) request.get("currency");

            // Handle multiple order IDs (comma-separated)
            String[] orderIdStrings = orderIdsStr.split(",");
            Long[] orderIds = new Long[orderIdStrings.length];
            for (int i = 0; i < orderIdStrings.length; i++) {
                orderIds[i] = Long.valueOf(orderIdStrings[i].trim());
            }

            // Verify all orders exist and belong to same retailer
            Long retailerId = null;
            for (Long orderId : orderIds) {
                Order order = orderRepository.findById(orderId).orElse(null);
                if (order == null) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Order not found: " + orderId));
                }
                if (retailerId == null) {
                    retailerId = order.getRetailer().getId();
                } else if (!retailerId.equals(order.getRetailer().getId())) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Orders must belong to same retailer"));
                }
            }

            // Initialize Razorpay client
            RazorpayClient razorpay = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

            // Create order options
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amount.toString()); // amount in paisa
            orderRequest.put("currency", currency);
            orderRequest.put("receipt", "orders_" + String.join("_", orderIdStrings));

            // Create order
            com.razorpay.Order razorpayOrder = razorpay.orders.create(orderRequest);

            // Update all orders with payment intent id
            for (Long orderId : orderIds) {
                Order order = orderRepository.findById(orderId).orElse(null);
                if (order != null) {
                    order.setPaymentIntentId(razorpayOrder.get("id"));
                    order.setPaymentStatus("PENDING");
                    orderRepository.save(order);
                }
            }

            // Return order details
            return ResponseEntity.ok(Map.of(
                "id", razorpayOrder.get("id").toString(),
                "amount", razorpayOrder.get("amount").toString(),
                "currency", razorpayOrder.get("currency").toString(),
                "orderIds", orderIdsStr // Return the order IDs for verification
            ));

        } catch (RazorpayException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Payment order creation failed: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    @PostMapping("/verify-payment")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, Object> request) {
        try {
            String razorpayOrderId = (String) request.get("razorpay_order_id");
            String razorpayPaymentId = (String) request.get("razorpay_payment_id");
            String razorpaySignature = (String) request.get("razorpay_signature");
            String orderIdsStr = request.get("orderId").toString();

            // Handle multiple order IDs (comma-separated)
            String[] orderIdStrings = orderIdsStr.split(",");
            Long[] orderIds = new Long[orderIdStrings.length];
            for (int i = 0; i < orderIdStrings.length; i++) {
                orderIds[i] = Long.valueOf(orderIdStrings[i].trim());
            }

            // Create signature verification data
            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id", razorpayOrderId);
            attributes.put("razorpay_payment_id", razorpayPaymentId);
            attributes.put("razorpay_signature", razorpaySignature);

            // Verify signature - throws exception if invalid
            com.razorpay.Utils.verifyPaymentSignature(attributes, razorpayKeySecret);

            // If no exception, payment is valid - update all orders
            Order firstOrder = null;
            for (Long orderId : orderIds) {
                Order order = orderRepository.findById(orderId).orElse(null);
                if (order != null) {
                    order.setPaymentStatus("PAID");
                    orderRepository.save(order);
                    if (firstOrder == null) {
                        firstOrder = order; // Use first order for email
                    }
                    System.out.println("=== PAYMENT SUCCESSFUL - ORDER UPDATED ===");
                    System.out.println("Order ID: " + order.getId() + " marked as PAID");
                }
            }

            // Send payment receipt email for the first order (or all orders)
            if (firstOrder != null) {
                System.out.println("=== PAYMENT RECEIPT EMAIL DEBUG START ===");
                System.out.println("PAYMENT VERIFICATION SUCCESSFUL - ATTEMPTING EMAIL SEND");
                System.out.println("Multiple orders paid: " + orderIdsStr);

                if (firstOrder.getRetailer() != null) {
                    System.out.println("Retailer found: " + firstOrder.getRetailer().getName());
                    System.out.println("Retailer email: " + firstOrder.getRetailer().getEmail());

                    if (firstOrder.getRetailer().getEmail() != null && !firstOrder.getRetailer().getEmail().trim().isEmpty()) {
                        System.out.println("Email validation passed. Generating PDF receipt...");

                        try {
                            byte[] pdfBytes = pdfService.generatePaymentReceipt(firstOrder, firstOrder.getRetailer());
                            System.out.println("PDF generated successfully, size: " + pdfBytes.length + " bytes");

                            System.out.println("Calling email service...");
                            emailService.sendPaymentReceiptEmail(
                                firstOrder.getRetailer().getEmail(),
                                firstOrder.getRetailer().getName(),
                                "MULTIPLE-" + orderIdsStr.replace(",", "-"),
                                pdfBytes
                            );
                            System.out.println("EMAIL SERVICE CALLED SUCCESSFULLY");
                            System.out.println("Payment receipt email should be sent to: " + firstOrder.getRetailer().getEmail());
                        } catch (Exception e) {
                            System.err.println("ERROR in PDF generation or email sending: " + e.getMessage());
                            e.printStackTrace();
                            // Try to send a simple text email as fallback
                            try {
                                System.out.println("Attempting fallback simple email...");
                                emailService.sendOrderStatusNotification(
                                    firstOrder.getRetailer().getEmail(),
                                    "MULTIPLE-" + orderIdsStr.replace(",", "-"),
                                    "PAYMENT RECEIPT - PDF generation failed, but payment was successful for orders: " + orderIdsStr
                                );
                                System.out.println("Fallback email sent successfully");
                            } catch (Exception fallbackE) {
                                System.err.println("Fallback email also failed: " + fallbackE.getMessage());
                            }
                        }
                    } else {
                        System.out.println("ERROR: Retailer email is null or empty");
                    }
                } else {
                    System.out.println("ERROR: Retailer is null for orders: " + orderIdsStr);
                }
                System.out.println("=== END PAYMENT RECEIPT EMAIL DEBUG ===");
            }

            return ResponseEntity.ok(Map.of("success", true, "message", "Payment verified successfully"));

        } catch (RazorpayException e) {
            // Update all orders status to failed
            String orderIdsStr = request.get("orderId").toString();
            String[] orderIdStrings = orderIdsStr.split(",");
            for (String orderIdStr : orderIdStrings) {
                try {
                    Long orderId = Long.valueOf(orderIdStr.trim());
                    Order order = orderRepository.findById(orderId).orElse(null);
                    if (order != null) {
                        order.setPaymentStatus("FAILED");
                        orderRepository.save(order);
                    }
                } catch (Exception ex) {
                    System.err.println("Error updating order status for: " + orderIdStr);
                }
            }

            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Payment verification failed"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Internal server error: " + e.getMessage()));
        }
    }

    @GetMapping("/key")
    public ResponseEntity<?> getRazorpayKey() {
        return ResponseEntity.ok(Map.of("key", razorpayKeyId));
    }

    @PostMapping("/test-email")
    public ResponseEntity<?> testEmail(@RequestBody Map<String, String> request) {
        try {
            String toEmail = request.get("email");
            System.out.println("Testing email send to: " + toEmail);

            // First test simple email
            System.out.println("Testing simple email first...");
            emailService.sendOrderStatusNotification(toEmail, "TEST-001", "TESTING");

            // Then test PDF email
            System.out.println("Testing PDF email...");
            byte[] testPdf = pdfService.generatePaymentReceipt(null, null); // Will handle null gracefully

            emailService.sendPaymentReceiptEmail(
                toEmail,
                "Test User",
                "TEST-001",
                testPdf
            );

            return ResponseEntity.ok(Map.of("message", "Test emails sent successfully"));

        } catch (Exception e) {
            System.err.println("Test email failed: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Test email failed: " + e.getMessage()));
        }
    }
}
