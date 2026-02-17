package com.example.demo.Controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.entity.Product;
import com.example.demo.entity.User;
import com.example.demo.enums.Role;
import com.example.demo.enums.VerificationStatus;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.UserRepository;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "http://localhost:3000")
public class ProductController {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    // ================= ADD PRODUCT (ONLY FARMER) =================
    @PostMapping
    public Product addProduct(
            @RequestParam String name,
            @RequestParam double price,
            @RequestParam int quantity,
            @RequestParam String category,          // ✅ CATEGORY ADDED
            @RequestParam Long farmerId,
            @RequestParam MultipartFile image
    ) throws IOException {

        User farmer = userRepository.findById(farmerId)
                .orElseThrow(() -> new RuntimeException("Farmer not found"));

        // ✅ ROLE CHECK
        if (farmer.getRole() != Role.FARMER) {
            throw new RuntimeException("Only FARMER can add products");
        }

        // ✅ VERIFICATION CHECK - Temporarily disabled for testing
        // if (farmer.getVerificationStatus() != VerificationStatus.VERIFIED) {
        //     throw new RuntimeException("Your account is not verified by admin.");
        // }

        // ================= IMAGE UPLOAD =================
        String uploadDir = "uploads";
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String fileName = System.currentTimeMillis() + "_" + image.getOriginalFilename();
        Path filePath = Paths.get(uploadDir, fileName);
        Files.write(filePath, image.getBytes());

        // ================= SAVE PRODUCT =================
        Product product = new Product();
        product.setName(name);
        product.setPrice(price);
        product.setQuantity(quantity);
        product.setCategory(category);             // ✅ SAVE CATEGORY
        product.setImageUrl("/uploads/" + fileName);
        product.setFarmer(farmer);

        return productRepository.save(product);
    }

    // ================= GET ALL PRODUCTS =================
    @GetMapping
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    // ================= GET PRODUCTS BY FARMER =================
    @GetMapping("/farmer/{farmerId}")
    public List<Product> getProductsByFarmer(@PathVariable Long farmerId) {
        return productRepository.findByFarmerId(farmerId);
    }

    // ================= GET PRODUCTS BY CATEGORY =================
    @GetMapping("/category/{category}")
    public List<Product> getProductsByCategory(@PathVariable String category) {
        return productRepository.findByCategory(category);
    }

    // ================= GET PRODUCT COUNT =================
    @GetMapping("/count")
    public long getProductCount() {
        return productRepository.count();
    }

    // ================= UPDATE PRODUCT (ONLY FARMER) =================
    @PutMapping("/{productId}")
    public Product updateProduct(
            @PathVariable Long productId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Double price,
            @RequestParam(required = false) Integer quantity,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) MultipartFile image,
            @RequestParam Long farmerId
    ) throws IOException {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        User farmer = userRepository.findById(farmerId)
                .orElseThrow(() -> new RuntimeException("Farmer not found"));

        // ✅ ROLE CHECK
        if (farmer.getRole() != Role.FARMER) {
            throw new RuntimeException("Only FARMER can update products");
        }

        // ✅ CHECK IF FARMER OWNS THE PRODUCT
        if (!product.getFarmer().getId().equals(farmerId)) {
            throw new RuntimeException("You can only update your own products");
        }

        // ================= UPDATE FIELDS =================
        if (name != null) product.setName(name);
        if (price != null) product.setPrice(price);
        if (quantity != null) product.setQuantity(quantity);
        if (category != null) product.setCategory(category);

        // ================= IMAGE UPDATE =================
        if (image != null && !image.isEmpty()) {
            String uploadDir = "uploads";
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String fileName = System.currentTimeMillis() + "_" + image.getOriginalFilename();
            Path filePath = Paths.get(uploadDir, fileName);
            Files.write(filePath, image.getBytes());
            product.setImageUrl("/uploads/" + fileName);
        }

        return productRepository.save(product);
    }
}
