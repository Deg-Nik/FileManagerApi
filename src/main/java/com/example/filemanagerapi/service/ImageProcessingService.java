package com.example.filemanagerapi.service;

import com.example.filemanagerapi.exception.FileStorageException;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Сервис для обработки изображений
 */
@Service
@Slf4j
public class ImageProcessingService {

    @Value("${app.thumbnail.width}")
    private int thumbnailWidth;

    @Value("${app.thumbnail.height}")
    private int thumbnailHeight;

    @Value("${app.upload.thumbnails}")
    private String thumbnailsDirectory;

    /**
     * Создать thumbnail для изображения
     * @return путь к thumbnail
     */
    public String createThumbnail(MultipartFile file, String originalFilePath) {
        try {
            log.info("Creating thumbnail for: {}", originalFilePath);

            // Генерировать имя thumbnail
            String thumbnailFilename = "thumb_" +
                    Paths.get(originalFilePath).getFileName().toString();

            Path thumbnailPath = Paths.get(thumbnailsDirectory, thumbnailFilename);

            // Создать thumbnail
            Thumbnails.of(file.getInputStream())
                    .size(thumbnailWidth, thumbnailHeight)
                    .outputFormat("jpg")
                    .outputQuality(0.8)
                    .toFile(thumbnailPath.toFile());

            log.info("✅ Thumbnail created: {}", thumbnailPath);

            return thumbnailPath.toString();

        } catch (IOException e) {
            log.error("❌ Failed to create thumbnail", e);
            throw new FileStorageException("Failed to create thumbnail", e);
        }
    }

    /**
     * Resize изображения
     */
    public byte[] resizeImage(Path imagePath, int width, int height,
                              boolean keepAspectRatio) {
        try {
            log.info("Resizing image: {} to {}x{}", imagePath, width, height);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            Thumbnails.Builder<?> builder = Thumbnails.of(imagePath.toFile())
                    .size(width, height);

            if (!keepAspectRatio) {
                builder.keepAspectRatio(false);
            }

            builder.outputFormat("jpg")
                    .outputQuality(0.85)
                    .toOutputStream(outputStream);

            log.info("✅ Image resized successfully");

            return outputStream.toByteArray();

        } catch (IOException e) {
            log.error("❌ Failed to resize image", e);
            throw new FileStorageException("Failed to resize image", e);
        }
    }

    /**
     * Создать resized копию и сохранить
     */
    public String createResizedCopy(Path originalPath, int width, int height,
                                    boolean keepAspectRatio) {
        try {
            // Генерировать имя для resized файла
            String originalFilename = originalPath.getFileName().toString();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String baseName = originalFilename.substring(0, originalFilename.lastIndexOf("."));

            String resizedFilename = baseName + "_" + width + "x" + height + extension;
            Path resizedPath = originalPath.getParent().resolve(resizedFilename);

            // Resize и сохранить
            byte[] resizedBytes = resizeImage(originalPath, width, height, keepAspectRatio);
            Files.write(resizedPath, resizedBytes);

            log.info("✅ Resized copy created: {}", resizedPath);

            return resizedPath.toString();

        } catch (IOException e) {
            log.error("❌ Failed to create resized copy", e);
            throw new FileStorageException("Failed to create resized copy", e);
        }
    }
}