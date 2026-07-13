package com.aicodereview.backend.util;

import com.aicodereview.backend.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class FileStorageUtil {

    @Value("${app.upload.dir}")
    private String uploadDir;

    private static final List<String> IGNORED_FOLDERS = List.of(
            "target", "build", "bin", "out", "node_modules", ".git", ".idea", "__MACOSX"
    );

    public String createProjectFolder(Long userId) {
        String folderName = "user_" + userId + "_" + UUID.randomUUID().toString().substring(0, 8);
        Path path = Paths.get(uploadDir, folderName);
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new ApiException("Could not create upload folder: " + e.getMessage());
        }
        return path.toString();
    }

    public int saveJavaFiles(MultipartFile[] files, String folderPath) {
        int count = 0;
        for (MultipartFile file : files) {
            String originalName = file.getOriginalFilename();

            if (originalName == null || !originalName.toLowerCase().endsWith(".java")) {
                continue;
            }

            String safeName = Paths.get(originalName).getFileName().toString();
            Path target = Paths.get(folderPath, safeName);

            try {
                Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
                count++;
            } catch (IOException e) {
                throw new ApiException("Failed to save file " + safeName + ": " + e.getMessage());
            }
        }

        if (count == 0) {
            throw new ApiException("No valid .java files found in upload");
        }
        return count;
    }

    public int extractZip(MultipartFile zipFile, String folderPath) {
        int count = 0;

        try (ZipInputStream zis = new ZipInputStream(zipFile.getInputStream())) {
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    zis.closeEntry();
                    continue;
                }

                String entryName = entry.getName();

                if (isIgnored(entryName) || !entryName.toLowerCase().endsWith(".java")) {
                    zis.closeEntry();
                    continue;
                }

                String flatName = Paths.get(entryName).getFileName().toString();
                Path target = Paths.get(folderPath, flatName);

                Files.copy(zis, target, StandardCopyOption.REPLACE_EXISTING);
                count++;
                zis.closeEntry();
            }

        } catch (IOException e) {
            throw new ApiException("Failed to extract ZIP: " + e.getMessage());
        }

        if (count == 0) {
            throw new ApiException("No .java files found inside the ZIP");
        }
        return count;
    }

    public String saveSnippet(String code, String fileName, String folderPath) {
        String safeName = fileName.toLowerCase().endsWith(".java") ? fileName : fileName + ".java";
        Path target = Paths.get(folderPath, safeName);

        try {
            Files.writeString(target, code);
        } catch (IOException e) {
            throw new ApiException("Failed to save snippet: " + e.getMessage());
        }
        return target.toString();
    }

    public List<String> listJavaFiles(String folderPath) {
        List<String> names = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(folderPath), "*.java")) {
            for (Path p : stream) {
                names.add(p.getFileName().toString());
            }
        } catch (IOException e) {
            throw new ApiException("Could not read project folder");
        }
        return names;
    }

    public String readFile(String folderPath, String fileName) {
        try {
            return Files.readString(Paths.get(folderPath, fileName));
        } catch (IOException e) {
            throw new ApiException("Could not read file: " + fileName);
        }
    }

    public void deleteFolder(String folderPath) {
        try (var paths = Files.walk(Paths.get(folderPath))) {
            paths.sorted((a, b) -> b.compareTo(a))
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException e) {
            throw new ApiException("Could not delete project folder");
        }
    }

    private boolean isIgnored(String entryName) {
        String normalized = entryName.replace("\\", "/");
        for (String folder : IGNORED_FOLDERS) {
            if (normalized.contains("/" + folder + "/") || normalized.startsWith(folder + "/")) {
                return true;
            }
        }
        return false;
    }
}