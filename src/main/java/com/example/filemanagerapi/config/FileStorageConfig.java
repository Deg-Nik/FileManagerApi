package com.example.filemanagerapi.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Конфигурация для File Storage
 * Автоматически создает папки при старте приложения
 */
@Configuration
public class FileStorageConfig {
    @Value("${app.upload.avatars}")
    private String avatarsDirectory;

    @Value("${app.upload.documents}")
    private String documentsDirectory;

    @Value("${app.upload.images}")
    private String imagesDirectory;

    @Value("${app.upload.thumbnails}")
    private String thumbnailsDirectory;

    /**
     * Создать папки при старте приложения
     */
    @PostConstruct
    public void init() throws IOException {
        createDirectoryIfNotExists(avatarsDirectory);
        createDirectoryIfNotExists(documentsDirectory);
        createDirectoryIfNotExists(imagesDirectory);
        createDirectoryIfNotExists(thumbnailsDirectory);
    }

    private void createDirectoryIfNotExists(String directory) throws IOException {
        Path path = Paths.get(directory);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
            System.out.println("Created directory: " + directory);
        }
    }
}
