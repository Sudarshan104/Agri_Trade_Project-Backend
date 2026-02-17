package com.example.demo.services;

import com.example.demo.entity.IssueReport;
import com.example.demo.entity.User;
import com.example.demo.repository.IssueReportRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IssueReportService {

    @Autowired
    private IssueReportRepository issueReportRepository;

    @Autowired
    private UserRepository userRepository;

    public IssueReport createIssueReport(Long userId, String subject, String description) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        IssueReport report = new IssueReport();
        report.setUser(user);
        report.setSubject(subject);
        report.setDescription(description);

        return issueReportRepository.save(report);
    }

    public List<IssueReport> getIssueReportsByUser(Long userId) {
        return issueReportRepository.findByUserId(userId);
    }

    public List<IssueReport> getAllIssueReports() {
        return issueReportRepository.findAllByOrderByCreatedAtDesc();
    }
}
