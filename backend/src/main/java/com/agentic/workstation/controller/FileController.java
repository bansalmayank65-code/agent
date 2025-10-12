package com.agentic.workstation.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;

@RestController
@RequestMapping("/files")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080"}, allowCredentials = "false")
public class FileController {

    @PostMapping("/list")
    public ResponseEntity<?> listDirectory(@RequestBody JsonNode request) {
        try {
            String directory = request.get("directory").asText();
            Path dirPath = Paths.get(directory);
            
            if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Directory does not exist"));
            }

            List<Map<String, Object>> files = Files.list(dirPath)
                .map(path -> {
                    try {
                        File file = path.toFile();
                        Map<String, Object> fileInfo = new HashMap<>();
                        fileInfo.put("name", file.getName());
                        fileInfo.put("path", path.toString());
                        fileInfo.put("isDirectory", file.isDirectory());
                        fileInfo.put("size", file.isDirectory() ? 0 : file.length());
                        fileInfo.put("lastModified", 
                            LocalDateTime.ofInstant(
                                java.time.Instant.ofEpochMilli(file.lastModified()),
                                java.time.ZoneId.systemDefault()
                            ).format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"))
                        );
                        return fileInfo;
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .sorted((a, b) -> {
                    // Directories first, then files, both alphabetically
                    boolean aIsDir = (Boolean) a.get("isDirectory");
                    boolean bIsDir = (Boolean) b.get("isDirectory");
                    if (aIsDir && !bIsDir) return -1;
                    if (!aIsDir && bIsDir) return 1;
                    return ((String) a.get("name")).compareToIgnoreCase((String) b.get("name"));
                })
                .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of("files", files));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to list directory: " + e.getMessage()));
        }
    }

    @PostMapping("/read")
    public ResponseEntity<?> readFile(@RequestBody JsonNode request) {
        try {
            String filePath = request.get("filePath").asText();
            Path path = Paths.get(filePath);
            
            if (!Files.exists(path) || Files.isDirectory(path)) {
                return ResponseEntity.badRequest().body(Map.of("error", "File does not exist"));
            }

            String content = Files.readString(path);
            return ResponseEntity.ok(Map.of("content", content));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to read file: " + e.getMessage()));
        }
    }

    @PostMapping("/write")
    public ResponseEntity<?> writeFile(@RequestBody JsonNode request) {
        try {
            String filePath = request.get("filePath").asText();
            String content = request.get("content").asText();
            Path path = Paths.get(filePath);
            
            // Create parent directories if they don't exist
            Files.createDirectories(path.getParent());
            
            Files.writeString(path, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to write file: " + e.getMessage()));
        }
    }

    @PostMapping("/delete")
    public ResponseEntity<?> deleteFile(@RequestBody JsonNode request) {
        try {
            String filePath = request.get("filePath").asText();
            Path path = Paths.get(filePath);
            
            if (!Files.exists(path)) {
                return ResponseEntity.badRequest().body(Map.of("error", "File does not exist"));
            }

            if (Files.isDirectory(path)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Cannot delete directories"));
            }

            Files.delete(path);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to delete file: " + e.getMessage()));
        }
    }
}