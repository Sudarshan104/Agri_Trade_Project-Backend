package com.example.demo.Controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.example.demo.entity.OrderStatus;
import com.example.demo.enums.Role;
import com.example.demo.enums.VerificationStatus;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.UserRepository;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "http://localhost:3000")
public class DashboardController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    // ================= ADMIN DASHBOARD =================
    @GetMapping("/admin")
    public Map<String, Object> getAdminStats() {
        System.out.println(">>> Fetching Admin Stats...");
        Map<String, Object> stats = new HashMap<>();

        // User Counts
        long userCount = userRepository.count();
        System.out.println("Total Users: " + userCount);
        stats.put("totalUsers", userCount);
        stats.put("totalFarmers", userRepository.countByRole(Role.FARMER));
        stats.put("totalRetailers", userRepository.countByRole(Role.RETAILER));
        stats.put("pendingVerifications", userRepository.countByVerificationStatus(VerificationStatus.PENDING));

        // Order Counts
        stats.put("totalOrders", orderRepository.count());

        // Revenue (sum of totalAmount for delivered/completed orders or all paid
        // orders)
        // Adjust status filter as per business logic. Here assume COMPLETED + DELIVERED
        // counts as real revenue.
        Double revenue = orderRepository.getTotalAdminRevenue();
        stats.put("totalRevenue", revenue != null ? revenue : 0.0);

        System.out.println("Admin Stats Response: " + stats);
        return stats;
    }

    // ================= FARMER DASHBOARD =================
    @GetMapping("/farmer/{farmerId}")
    public Map<String, Object> getFarmerStats(@PathVariable Long farmerId) {
        System.out.println(">>> Fetching Farmer Stats for ID: " + farmerId);
        Map<String, Object> stats = new HashMap<>();

        // Products
        long products = productRepository.countByFarmerId(farmerId);
        System.out.println("Farmer " + farmerId + " Products: " + products);
        stats.put("totalProducts", products);

        // Orders
        long orders = orderRepository.countByFarmerId(farmerId);
        System.out.println("Farmer " + farmerId + " Total Orders: " + orders);
        stats.put("totalOrders", orders);
        stats.put("pendingOrders", orderRepository.countByFarmerIdAndStatus(farmerId, OrderStatus.PLACED));

        // Revenue
        Double revenue = orderRepository.sumRevenueByFarmerId(farmerId, OrderStatus.DELIVERED);
        stats.put("totalRevenue", revenue != null ? revenue : 0.0);

        System.out.println("Farmer Stats Response: " + stats);
        return stats;
    }

    // ================= RETAILER DASHBOARD =================
    @GetMapping("/retailer/{retailerId}")
    public Map<String, Object> getRetailerStats(@PathVariable Long retailerId) {
        System.out.println(">>> Fetching Retailer Stats for ID: " + retailerId);
        Map<String, Object> stats = new HashMap<>();

        // Orders
        long orders = orderRepository.countByRetailerId(retailerId);
        System.out.println("Retailer " + retailerId + " Orders: " + orders);
        stats.put("totalOrders", orders);
        stats.put("pendingOrders", orderRepository.countByRetailerIdAndStatus(retailerId, OrderStatus.PLACED));

        // Expense (Revenue from their perspective)
        Double expense = orderRepository.sumRevenueByRetailerId(retailerId, OrderStatus.DELIVERED);
        stats.put("totalSpent", expense != null ? expense : 0.0);

        System.out.println("Retailer Stats Response: " + stats);
        return stats;
    }
}
