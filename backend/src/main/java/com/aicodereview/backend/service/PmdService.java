package com.aicodereview.backend.service;

import com.aicodereview.backend.entity.Review;
import com.aicodereview.backend.entity.ReviewFinding;
import com.aicodereview.backend.exception.ApiException;
import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.PmdAnalysis;
import net.sourceforge.pmd.lang.rule.RulePriority;
import net.sourceforge.pmd.reporting.Report;
import net.sourceforge.pmd.reporting.RuleViolation;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Service
public class PmdService {

    public List<ReviewFinding> analyze(String folderPath, Review review) {
        List<ReviewFinding> findings = new ArrayList<>();

        List<Path> javaFiles = collectJavaFiles(folderPath);
        if (javaFiles.isEmpty()) {
            return findings;
        }

        Path rulesetPath = extractRulesetToTemp();

        PMDConfiguration config = new PMDConfiguration();
        config.setDefaultLanguageVersion(
                config.getLanguageVersionDiscoverer()
                        .getDefaultLanguageVersion(
                                net.sourceforge.pmd.lang.LanguageRegistry.PMD.getLanguageById("java")
                        )
        );
        config.addRuleSet(rulesetPath.toString());
        config.setIgnoreIncrementalAnalysis(true);

        try (PmdAnalysis pmd = PmdAnalysis.create(config)) {
            for (Path file : javaFiles) {
                pmd.files().addFile(file);
            }

            Report report = pmd.performAnalysisAndCollectReport();

            for (RuleViolation violation : report.getViolations()) {
                findings.add(ReviewFinding.builder()
                        .review(review)
                        .tool("PMD")
                        .severity(mapPriority(violation.getRule().getPriority()))
                        .issue(violation.getDescription())
                        .explanation("PMD rule violated: " + violation.getRule().getName()
                                + " (" + violation.getRule().getRuleSetName() + ")")
                        .suggestion(buildSuggestion(violation))
                        .fileName(Paths.get(violation.getFileId().getAbsolutePath())
                                .getFileName().toString())
                        .lineNumber(violation.getBeginLine())
                        .build());
            }

        } catch (Exception e) {
            throw new ApiException("PMD analysis failed: " + e.getMessage());
        }

        return findings;
    }

    private Path extractRulesetToTemp() {
        try (InputStream in = new ClassPathResource("pmd-ruleset.xml").getInputStream()) {
            Path temp = Files.createTempFile("pmd-ruleset", ".xml");
            Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
            temp.toFile().deleteOnExit();
            return temp;
        } catch (Exception e) {
            throw new ApiException("Could not load PMD ruleset: " + e.getMessage());
        }
    }

    private List<Path> collectJavaFiles(String folderPath) {
        try (Stream<Path> paths = Files.walk(Paths.get(folderPath))) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().toLowerCase().endsWith(".java"))
                    .toList();
        } catch (Exception e) {
            throw new ApiException("Could not read project files for PMD analysis");
        }
    }

    private String mapPriority(RulePriority priority) {
        return switch (priority) {
            case HIGH -> "HIGH";
            case MEDIUM_HIGH -> "HIGH";
            case MEDIUM -> "MEDIUM";
            case MEDIUM_LOW -> "LOW";
            case LOW -> "LOW";
        };
    }

    private String buildSuggestion(RuleViolation violation) {
        String desc = violation.getRule().getDescription();
        if (desc != null && !desc.isBlank()) {
            return desc.trim().replaceAll("\\s+", " ");
        }
        return "Refactor the code to comply with the " + violation.getRule().getName() + " rule.";
    }
}