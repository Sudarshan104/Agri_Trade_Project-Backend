package com.example.demo.services;

import com.example.demo.entity.Order;
import com.example.demo.entity.User;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.HorizontalAlignment;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class PdfService {

    public byte[] generatePaymentReceipt(Order order, User retailer) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            PdfWriter writer = new PdfWriter(outputStream);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            // Title
            Paragraph title = new Paragraph("PAYMENT RECEIPT")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(20)
                    .setBold();
            document.add(title);

            // Platform name
            Paragraph platform = new Paragraph("Farmer Dashboard Platform")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(14);
            document.add(platform);

            document.add(new LineSeparator(null));

            // Receipt details - handle null values gracefully
            if (order != null) {
                document.add(new Paragraph("Receipt Number: RCP-" + order.getId()));
                document.add(new Paragraph("Order ID: ORD-" + order.getId()));
                document.add(new Paragraph("Date: " + (order.getOrderDate() != null ? order.getOrderDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "N/A")));
                document.add(new Paragraph("Payment ID: " + (order.getPaymentIntentId() != null ? order.getPaymentIntentId() : "N/A")));
                document.add(new Paragraph("Payment Status: " + (order.getPaymentStatus() != null ? order.getPaymentStatus() : "N/A")));
                document.add(new Paragraph("Amount: ₹" + (order.getTotalAmount() != null ? order.getTotalAmount().toString() : "0.00")));
            } else {
                // Test receipt
                document.add(new Paragraph("Receipt Number: TEST-RCP-001"));
                document.add(new Paragraph("Order ID: TEST-ORD-001"));
                document.add(new Paragraph("Date: " + java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
                document.add(new Paragraph("Payment ID: TEST-PAYMENT-ID"));
                document.add(new Paragraph("Payment Status: TEST"));
                document.add(new Paragraph("Amount: ₹100.00"));
            }

            if (retailer != null) {
                document.add(new Paragraph("Retailer Name: " + retailer.getName()));
                document.add(new Paragraph("Retailer Email: " + retailer.getEmail()));
            } else {
                // Test retailer
                document.add(new Paragraph("Retailer Name: Test User"));
                document.add(new Paragraph("Retailer Email: test@example.com"));
            }

            document.add(new Paragraph("Currency: INR"));

            document.add(new LineSeparator(null));

            // Footer message
            Paragraph footer = new Paragraph("Thank you for your payment!\nThis is an electronically generated receipt.")
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(footer);

            document.close();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error generating PDF receipt: " + e.getMessage());
        }

        return outputStream.toByteArray();
    }
}
