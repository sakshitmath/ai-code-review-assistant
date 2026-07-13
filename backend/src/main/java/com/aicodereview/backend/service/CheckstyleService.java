package com.aicodereview.backend.service;

import com.aicodereview.backend.entity.Review;
import com.aicodereview.backend.entity.ReviewFinding;
import com.aicodereview.backend.exception.ApiException;
import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.PropertiesExpander;
import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.AuditListener;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import com.puppycrawl.tools.checkstyle.api.SeverityLevel;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

@Service
public class CheckstyleService {

    public List<ReviewFinding> analyze(String folderPath, Review review) {
        List<ReviewFinding> findings = new ArrayList<>();

        List<File> javaFiles = collectJavaFiles(folderPath);
        if (javaFiles.isEmpty()) {
            return findings;
        }

        Checker checker = new Checker();

        try {
            checker.setModuleClassLoader(Checker.class.getClassLoader());

            InputStream configStream = new ClassPathResource("checkstyle.xml").getInputStream();
            Configuration config = ConfigurationLoader.loadConfiguration(
                    new org.xml.sax.InputSource(configStream),
                    new PropertiesExpander(new Properties()),
                    ConfigurationLoader.IgnoredModulesOptions.OMIT
            );
            checker.configure(config);

            checker.addListener(new AuditListener() {
                @Override
                public void auditStarted(AuditEvent event) { }

                @Override
                public void auditFinished(AuditEvent event) { }

                @Override
                public void fileStarted(AuditEvent event) { }

                @Override
                public void fileFinished(AuditEvent event) { }

                @Override
                public void addError(AuditEvent event) {
                    findings.add(ReviewFinding.builder()
                            .review(review)
                            .tool("CHECKSTYLE")
                            .severity(mapSeverity(event.getSeverityLevel()))
                            .issue(event.getMessage())
                            .explanation("Coding standard violation detected by Checkstyle: "
                                    + shortRuleName(event.getSourceName()))
                            .suggestion("Fix the reported style issue to follow standard Java conventions.")
                            .fileName(Paths.get(event.getFileName()).getFileName().toString())
                            .lineNumber(event.getLine())
                            .build());
                }

                @Override
                public void addException(AuditEvent event, Throwable throwable) { }
            });

            checker.process(javaFiles);

        } catch (Exception e) {
            throw new ApiException("Checkstyle analysis failed: " + e.getMessage());
        } finally {
            checker.destroy();
        }

        return findings;
    }

    private List<File> collectJavaFiles(String folderPath) {
        try (Stream<Path> paths = Files.walk(Paths.get(folderPath))) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().toLowerCase().endsWith(".java"))
                    .map(Path::toFile)
                    .toList();
        } catch (Exception e) {
            throw new ApiException("Could not read project files for analysis");
        }
    }

    private String mapSeverity(SeverityLevel level) {
        return switch (level) {
            case ERROR -> "HIGH";
            case WARNING -> "MEDIUM";
            case INFO -> "LOW";
            default -> "LOW";
        };
    }

    private String shortRuleName(String sourceName) {
        if (sourceName == null) return "Unknown";
        int idx = sourceName.lastIndexOf('.');
        return idx >= 0 ? sourceName.substring(idx + 1) : sourceName;
    }
}