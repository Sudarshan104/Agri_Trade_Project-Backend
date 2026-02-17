package com.example.demo.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendOrderStatusNotification(String toEmail, String orderId, String status) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Order Status Update - Order #" + orderId);
        message.setText("Your order #" + orderId + " status has been updated to: " + status +
                "\n\nThank you for using our platform!");

        mailSender.send(message);
    }

    public void sendWelcomeEmail(String toEmail, String userName, String role) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Welcome to Farmer Dashboard Platform!");
        message.setText("Dear " + userName + ",\n\n" +
                "Welcome to our Farmer Dashboard Platform! You have successfully registered as a " + role + ".\n\n" +
                "Thank you for joining our community. We look forward to your contributions!\n\n" +
                "Best regards,\n" +
                "Farmer Dashboard Team");

        mailSender.send(message);
    }

    public void sendDeliveryConfirmationEmail(String toEmail, String farmerName, String productName, String orderId) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Product Delivery Confirmation");
        message.setText("Dear " + farmerName + ",\n\n" +
                "Congratulations! Your product '" + productName + "' has been successfully delivered.\n\n" +
                "Order ID: " + orderId + "\n\n" +
                "Thank you for using our platform. We appreciate your partnership!\n\n" +
                "Best regards,\n" +
                "Farmer Dashboard Team");

        mailSender.send(message);
    }

    public void sendPaymentReceiptEmail(String toEmail, String retailerName, String orderId, byte[] receiptBytes) {
        System.out.println("=== PAYMENT RECEIPT EMAIL DEBUG ===");
        System.out.println("METHOD ENTRY: sendPaymentReceiptEmail called");
        System.out.println("Attempting to send payment receipt email to: " + toEmail);
        System.out.println("Retailer name: " + retailerName);
        System.out.println("Order ID: " + orderId);
        System.out.println("Receipt bytes length: " + (receiptBytes != null ? receiptBytes.length : "null"));

        // Validate PDF bytes
        if (receiptBytes == null || receiptBytes.length == 0) {
            System.err.println("ERROR: PDF bytes are null or empty - cannot send attachment");
            throw new RuntimeException("PDF receipt generation failed - no content to attach");
        }

        // Check if PDF starts with PDF header
        if (receiptBytes.length < 4 || receiptBytes[0] != '%' || receiptBytes[1] != 'P' || receiptBytes[2] != 'D'
                || receiptBytes[3] != 'F') {
            System.err.println("ERROR: Generated content does not appear to be a valid PDF file");
            System.err.println("First 10 bytes: " + java.util.Arrays
                    .toString(java.util.Arrays.copyOf(receiptBytes, Math.min(10, receiptBytes.length))));
        } else {
            System.out.println("PDF validation passed - content appears to be valid PDF");
        }

        try {
            // First try to send a simple text email to verify basic email functionality
            System.out.println("Sending simple text email first...");
            SimpleMailMessage testMessage = new SimpleMailMessage();
            testMessage.setTo(toEmail);
            testMessage.setSubject("Payment Receipt - Order #" + orderId);
            testMessage.setText("Dear " + retailerName + ",\n\n" +
                    "Thank you for your payment! Your payment receipt for Order #" + orderId + " is attached.\n\n" +
                    "This receipt serves as confirmation of your successful payment.\n\n" +
                    "Best regards,\n" +
                    "Farmer Dashboard Team");
            mailSender.send(testMessage);
            System.out.println("Simple text email sent successfully");

            // Now try to send the email with PDF attachment
            System.out.println("Now attempting to send email with PDF attachment...");
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(toEmail);
            helper.setSubject("Payment Receipt - Order #" + orderId);

            // Create HTML email body
            String htmlBody = "<html><body>" +
                    "<h2>Payment Receipt</h2>" +
                    "<p>Dear " + retailerName + ",</p>" +
                    "<p>Thank you for your payment! Please find your payment receipt for Order #" + orderId
                    + " attached.</p>" +
                    "<p>This receipt serves as confirmation of your successful payment.</p>" +
                    "<br>" +
                    "<p>Best regards,<br>Farmer Dashboard Team</p>" +
                    "</body></html>";

            helper.setText(htmlBody, true); // true indicates HTML content

            // Attach PDF file using InputStreamSource
            if (receiptBytes != null && receiptBytes.length > 0) {
                System.out.println("Attaching PDF file...");
                org.springframework.core.io.InputStreamSource pdfSource = new org.springframework.core.io.InputStreamSource() {
                    @Override
                    public java.io.InputStream getInputStream() throws java.io.IOException {
                        return new java.io.ByteArrayInputStream(receiptBytes);
                    }
                };
                helper.addAttachment("Payment_Receipt_Order_" + orderId + ".pdf", pdfSource, "application/pdf");
                System.out.println("PDF attachment added successfully");
            } else {
                System.out.println("WARNING: PDF bytes are null or empty, skipping attachment");
            }

            System.out.println("About to send email with PDF attachment...");
            mailSender.send(message);
            System.out.println("Email with PDF attachment sent successfully to: " + toEmail);

        } catch (MessagingException e) {
            System.err.println("MessagingException in sendPaymentReceiptEmail: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to send payment receipt email", e);
        } catch (Exception e) {
            System.err.println("Unexpected exception in sendPaymentReceiptEmail: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to send payment receipt email", e);
        }
    }

    public void sendOrderStatusUpdateNotification(String toEmail, String retailerName, String orderId, String newStatus,
            String additionalInfo) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Order Status Update - Order #" + orderId);
        message.setText("Dear " + retailerName + ",\n\n" +
                "Your order #" + orderId + " status has been updated to: " + newStatus + "\n\n" +
                (additionalInfo != null ? additionalInfo + "\n\n" : "") +
                "Thank you for using our platform!\n\n" +
                "Best regards,\n" +
                "Farmer Dashboard Team");

        mailSender.send(message);
    }

    public void sendPickupOtpNotification(String toEmail, String farmerName, String orderId, String pickupOtp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Action Required: Pickup OTP for Order #" + orderId);
        message.setText("Dear " + farmerName + ",\n\n" +
                "A delivery agent has been assigned to pick up your product for Order #" + orderId + ".\n\n" +
                "Please provide the following Pickup OTP to the agent when they arrive:\n" +
                "Pickup OTP: " + pickupOtp + "\n\n" +
                "Thank you for your cooperation!\n\n" +
                "Best regards,\n" +
                "Farmer Dashboard Team");

        mailSender.send(message);
    }
}
