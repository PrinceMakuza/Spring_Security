package com.ecommerce.service;

import com.ecommerce.util.PerformanceMonitor;
import com.ecommerce.util.ValidationRunner;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;

/**
 * ReportService handles the orchestration of system reports.
 * Responsible for running measurements and saving them to the file system.
 */
public class ReportService {

    public String generatePerformanceReport() throws Exception {
        PerformanceMonitor monitor = new PerformanceMonitor();
        String content = monitor.runTestsAndGenerateReport();
        String filePath = Paths.get("performance_report.txt").toAbsolutePath().toString();
        
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(content);
        }
        return filePath;
    }

    public String generateValidationReport() throws Exception {
        ValidationRunner runner = new ValidationRunner();
        String content = runner.runTestsAndGenerateReport();
        String filePath = Paths.get("validation_report.txt").toAbsolutePath().toString();
        
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(content);
        }
        return filePath;
    }
}
