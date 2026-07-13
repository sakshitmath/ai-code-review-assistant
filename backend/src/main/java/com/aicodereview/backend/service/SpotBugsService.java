package com.aicodereview.backend.service;

import com.aicodereview.backend.entity.Review;
import com.aicodereview.backend.entity.ReviewFinding;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.FindBugs2;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.config.UserPreferences;
import org.springframework.stereotype.Service;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Service
public class SpotBugsService {

    public List<ReviewFinding> analyze(String folderPath, Review review) {
        List<ReviewFinding> findings = new ArrayList<>();
        Path classesDir = Paths.get(folderPath, "classes");

        try {
            List<Path> javaFiles = collectJavaFiles(folderPath);
            if (javaFiles.isEmpty()) {
                return findings;
            }

            Files.createDirectories(classesDir);

            boolean compiled = compile(javaFiles, classesDir);
            if (!compiled) {
                deleteDirectory(classesDir);
                return findings;
            }

            List<Path> classFiles = collectClassFiles(classesDir);
            if (classFiles.isEmpty()) {
                deleteDirectory(classesDir);
                return findings;
            }

            Project project = new Project();
            for (Path classFile : classFiles) {
                project.addFile(classFile.toAbsolutePath().toString());
            }

            edu.umd.cs.findbugs.BugCollectionBugReporter reporter =
                    new edu.umd.cs.findbugs.BugCollectionBugReporter(project);
            reporter.setPriorityThreshold(Priorities.LOW_PRIORITY);
            edu.umd.cs.findbugs.BugCollection bugCollection = reporter.getBugCollection();

            try (FindBugs2 findBugs = new FindBugs2()) {
                findBugs.setProject(project);
                findBugs.setBugReporter(reporter);
                findBugs.setDetectorFactoryCollection(
                        edu.umd.cs.findbugs.DetectorFactoryCollection.instance());
                findBugs.setUserPreferences(UserPreferences.createDefaultUserPreferences());
                findBugs.setNoClassOk(true);

                findBugs.execute();
            }

            for (BugInstance bug : bugCollection.getCollection()) {
                var sourceLine = bug.getPrimarySourceLineAnnotation();

                findings.add(ReviewFinding.builder()
                        .review(review)
                        .tool("SPOTBUGS")
                        .severity(mapPriority(bug.getPriority()))
                        .issue(bug.getMessageWithoutPrefix())
                        .explanation("SpotBugs detected a potential bug of type "
                                + bug.getBugPattern().getType()
                                + " in category " + bug.getBugPattern().getCategory())
                        .suggestion(cleanHtml(bug.getBugPattern().getDetailText()))
                        .fileName(sourceLine != null ? sourceLine.getSourceFile() : "unknown")
                        .lineNumber(sourceLine != null && sourceLine.getStartLine() > 0
                                ? sourceLine.getStartLine() : 1)
                        .build());
            }

            deleteDirectory(classesDir);

        } catch (Exception e) {
            deleteDirectory(classesDir);
            return findings;
        }

        return findings;
    }

    private boolean compile(List<Path> javaFiles, Path outputDir) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            return false;
        }

        List<String> args = new ArrayList<>();
        args.add("-d");
        args.add(outputDir.toAbsolutePath().toString());
        args.add("-nowarn");
        args.add("-proc:none");
        for (Path f : javaFiles) {
            args.add(f.toAbsolutePath().toString());
        }

        ByteArrayOutputStream errStream = new ByteArrayOutputStream();
        int result = compiler.run(null, null, errStream, args.toArray(new String[0]));

        return result == 0;
    }

    private List<Path> collectJavaFiles(String folderPath) {
        try (Stream<Path> paths = Files.walk(Paths.get(folderPath))) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().toLowerCase().endsWith(".java"))
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<Path> collectClassFiles(Path classesDir) {
        try (Stream<Path> paths = Files.walk(classesDir)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().toLowerCase().endsWith(".class"))
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    private void deleteDirectory(Path dir) {
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.sorted((a, b) -> b.compareTo(a))
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (Exception ignored) {
                        }
                    });
        } catch (Exception ignored) {
        }
    }

    private String mapPriority(int priority) {
        return switch (priority) {
            case Priorities.HIGH_PRIORITY -> "HIGH";
            case Priorities.NORMAL_PRIORITY -> "MEDIUM";
            default -> "LOW";
        };
    }

    private String cleanHtml(String html) {
        if (html == null || html.isBlank()) {
            return "Review the flagged code and fix the potential bug.";
        }
        String text = html.replaceAll("<[^>]*>", " ")
                .replaceAll("&nbsp;", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return text.length() > 1500 ? text.substring(0, 1500) + "..." : text;
    }
}