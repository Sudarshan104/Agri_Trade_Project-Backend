package com.example.demo.Controller;

import com.example.demo.entity.IssueReport;
import com.example.demo.services.IssueReportService;
import com.example.demo.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/issues")
@CrossOrigin(origins = "http://localhost:3000")
public class IssueReportController {

    @Autowired
    private IssueReportService issueReportService;

    @Autowired
    private JwtUtil jwtUtil;

    // Create issue report (Farmers and Retailers)
    @PostMapping("/report")
    public IssueReport createIssueReport(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, String> requestData) {

        String jwtToken = token.replace("Bearer ", "");
        Long userId = jwtUtil.extractUserId(jwtToken);

        String subject = requestData.get("subject");
        String description = requestData.get("description");

        return issueReportService.createIssueReport(userId, subject, description);
    }

    // Get user's issue reports
    @GetMapping("/my-reports")
    public List<IssueReport> getMyIssueReports(@RequestHeader("Authorization") String token) {
        String jwtToken = token.replace("Bearer ", "");
        Long userId = jwtUtil.extractUserId(jwtToken);

        return issueReportService.getIssueReportsByUser(userId);
    }
}
