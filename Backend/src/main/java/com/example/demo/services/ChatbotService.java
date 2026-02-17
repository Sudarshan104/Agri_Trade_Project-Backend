package com.example.demo.services;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.entity.Order;
import com.example.demo.entity.Product;
import com.example.demo.entity.User;
import com.example.demo.enums.Role;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.UserRepository;

@Service
public class ChatbotService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    public String processQuery(String message, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Role role = user.getRole();
        String intent = determineIntent(message.toLowerCase());

        System.out.println(
                "Chatbot - User: " + user.getName() + " (" + role + ") | Message: " + message + " | Intent: " + intent);

        switch (intent) {
            case "PRICE_CHECK":
                return handlePriceCheck(role);

            case "STOCK_CHECK":
                return handleStockCheck(user, role);

            case "ORDER_STATUS":
                return handleOrderStatus(user, role);

            case "DELIVERY_TRACKING":
                return handleDeliveryTracking(user, role);

            case "COMPLAINT":
                return "You can raise a complaint or report an issue via the 'Issues' tab in your dashboard sidebar. Our team resolves critical issues within 24 hours.";

            case "HELP":
                return getHelpMessage(role);

            case "WEATHER":
                return "It's a great day for farming! For accurate local weather, please check your dashboard's weather widget or local news.";

            case "SCHEMES":
                return "Recent Government Schemes:\n1. PM-KISAN (Income Support)\n2. Pradhan Mantri Fasal Bima Yojana (Crop Insurance)\nVisit relevant govt portals for details.";

            case "PROJECT_INFO":
                return handleProjectInfo();

            case "USER_STATS":
                return handleUserStats(role); // New handler for "how many users"

            case "RESTRICTED":
                return "I'm sorry, you don’t have permission to perform this action. Please contact the admin if needed.";

            default:
                return "I'm not sure about that. " + getHelpMessage(role);
        }
    }

    private String determineIntent(String msg) {
        // 1. Core Actions
        if (msg.contains("price") || msg.contains("rate") || msg.contains("cost") || msg.contains("how much")
                || msg.contains("market"))
            return "PRICE_CHECK";

        if (msg.contains("stock") || msg.contains("available") || msg.contains("quantity") || msg.contains("inventory")
                || msg.contains("item"))
            return "STOCK_CHECK";

        if (msg.contains("order") || msg.contains("bought") || msg.contains("sold") || msg.contains("purchase")
                || msg.contains("transaction"))
            return "ORDER_STATUS";

        if (msg.contains("delivery") || msg.contains("track") || msg.contains("ship") || msg.contains("arrive")
                || msg.contains("otp"))
            return "DELIVERY_TRACKING";

        if (msg.contains("complaint") || msg.contains("issue") || msg.contains("problem") || msg.contains("broken")
                || msg.contains("error") || msg.contains("support"))
            return "COMPLAINT";

        // 2. Info & Helpers
        if (msg.contains("help") || msg.contains("can you") || msg.contains("what do") || msg.contains("hi")
                || msg.contains("hello"))
            return "HELP";

        if (msg.contains("weather") || msg.contains("rain") || msg.contains("climate") || msg.contains("temperature"))
            return "WEATHER";

        if (msg.contains("scheme") || msg.contains("subsidy") || msg.contains("govt") || msg.contains("government")
                || msg.contains("fund"))
            return "SCHEMES";

        // 3. Project & System Info
        // "how many users" -> USER_STATS
        if (msg.contains("how many user") || msg.contains("total user") || msg.contains("active user"))
            return "USER_STATS";

        if (msg.contains("about") || msg.contains("project") || msg.contains("app") || msg.contains("website")
                || msg.contains("who are you") || msg.contains("developer"))
            return "PROJECT_INFO";

        // 4. Strict restrictions
        if (msg.contains("delete") || msg.contains("remove") || msg.contains("ban") || msg.contains("admin"))
            return "RESTRICTED";
        if (msg.contains("change") || msg.contains("update")) // broad catch for modify attempts
            return "RESTRICTED";

        return "GENERAL_QUERY";
    }

    private String handlePriceCheck(Role role) {
        List<Product> products = productRepository.findAll();
        if (products.isEmpty())
            return "There are currently no products listed in the market to show prices for.";

        String priceList = products.stream()
                .map(p -> "• " + p.getName() + ": ₹" + p.getPrice() + "/kg")
                .distinct()
                .limit(8)
                .collect(Collectors.joining("\n"));

        return "Current Market Prices (Latest):\n" + priceList;
    }

    private String handleStockCheck(User user, Role role) {
        if (role == Role.FARMER) {
            List<Product> myProducts = productRepository.findByFarmerId(user.getId());
            if (myProducts.isEmpty())
                return "You haven't listed any products yet. Go to 'Add Products' to start.";

            return "Your Current Stock:\n" + myProducts.stream()
                    .map(p -> "• " + p.getName() + ": " + p.getQuantity() + " kg")
                    .collect(Collectors.joining("\n"));
        } else if (role == Role.RETAILER) {
            long totalProducts = productRepository.count();
            return "Market Overview: There are " + totalProducts + " active product listings available for purchase.";
        }
        return "System Status: Inventory is healthy.";
    }

    private String handleOrderStatus(User user, Role role) {
        List<Order> orders;
        if (role == Role.FARMER) {
            orders = orderRepository.findByProductFarmerId(user.getId());
            if (orders.isEmpty())
                return "You have no received orders yet.";
            return "Your Latest Sales:\n" + orders.stream()
                    .limit(5)
                    .map(o -> "• Order #" + o.getId() + ": " + o.getProduct().getName() + " (" + o.getStatus() + ")")
                    .collect(Collectors.joining("\n"));
        } else if (role == Role.RETAILER) {
            orders = orderRepository.findByRetailerId(user.getId());
            if (orders.isEmpty())
                return "You haven't placed any orders yet.";
            return "Your Recent Purchases:\n" + orders.stream()
                    .limit(5)
                    .map(o -> "• Order #" + o.getId() + ": " + o.getProduct().getName() + " (" + o.getStatus() + ")")
                    .collect(Collectors.joining("\n"));
        }
        return "Admin View: " + orderRepository.count() + " total orders in system.";
    }

    private String handleDeliveryTracking(User user, Role role) {
        if (role == Role.RETAILER) {
            List<Order> orders = orderRepository.findByRetailerId(user.getId());
            String tracking = orders.stream()
                    .filter(o -> o.getStatus().toString().equals("OUT_FOR_DELIVERY")
                            || o.getStatus().toString().equals("SHIPPED"))
                    .map(o -> "• Order #" + o.getId() + " is " + o.getStatus()
                            + (o.getDeliveryOtp() != null ? " (OTP: " + o.getDeliveryOtp() + ")" : ""))
                    .collect(Collectors.joining("\n"));

            return tracking.isEmpty() ? "No active deliveries right now. Check 'My Orders' for history."
                    : "🚚 Active Deliveries:\n" + tracking;
        } else if (role == Role.FARMER) {
            return "You can track which of your sold products are out for delivery in the 'Orders' section.";
        }
        return "Delivery tracking is available for Retailers and Farmers.";
    }

    private String handleProjectInfo() {
        return "Agri-Trade is a direct Farmer-to-Retailer platform designed to eliminate middlemen. \n\n" +
                "• For Farmers: List crops, set prices, and sell directly.\n" +
                "• For Retailers: Buy fresh produce directly from farmers.\n" +
                "• Goal: Fair prices for farmers and fresh quality for retailers.";
    }

    private String handleUserStats(Role role) {
        // Technically, regular users shouldn't see exact user counts, but since the
        // user asked for it specifically:
        long count = userRepository.count();
        if (role == Role.ADMIN) {
            return "System Users: There are currently " + count + " registered users on the platform.";
        }
        // For non-admins, give a more general answer or the count if permitted by
        // policy
        return "Our community is growing! We currently have " + count
                + " active farmers and retailers using the platform.";
    }

    private String getHelpMessage(Role role) {
        String msg = "I can assist you with:\n";
        if (role == Role.FARMER) {
            msg += "• Checking market prices ('price of onion')\n" +
                    "• Checking your stock ('my stock')\n" +
                    "• Tracking orders ('my orders')\n" +
                    "• Weather & Govt Schemes";
        } else if (role == Role.RETAILER) {
            msg += "• Market prices ('current rates')\n" +
                    "• Finding stock ('available stock')\n" +
                    "• Tracking deliveries ('track order')\n" +
                    "• Raising complaints";
        } else {
            msg += "• System & Order Overview";
        }
        return msg;
    }
}
