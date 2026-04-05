package com.example.filemanagerapi.service;

import com.example.filemanagerapi.dto.FileInfoResponse;
import com.example.filemanagerapi.dto.FileUploadResponse;
import com.example.filemanagerapi.entity.FileMetadata;
import com.example.filemanagerapi.entity.User;
import com.example.filemanagerapi.exception.FileStorageException;
import com.example.filemanagerapi.repository.FileMetadataRepository;
import com.example.filemanagerapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

//import static org.springframework.http.codec.multipart.MultipartUtils.deleteFile;

/**
 * Главный сервис для работы с файлами
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

    private final FileMetadataRepository fileMetadataRepository;
    private final UserRepository userRepository;
    private final FileValidationService validationService;
    private final ImageProcessingService imageProcessingService;

    @Value("${app.upload.avatars}")
    private String avatarsDirectory;

    @Value("${app.upload.documents}")
    private String documentsDirectory;

    @Value("${app.upload.images}")
    private String imagesDirectory;

    /**
     * Upload аватара пользователя
     */
    @Transactional
    public FileUploadResponse uploadAvatar(MultipartFile file, Long userId) {
        log.info("Uploading avatar for user: {}", userId);

        // Валидация
        validationService.validateNotEmpty(file);
        validationService.validateSize(file);
        validationService.validateImageType(file);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Удалить старый аватар если есть
        if (user.getAvatarFileId() != null) {
            deleteFile(user.getAvatarFileId());
        }

        // Сохранить новый аватар
        FileMetadata metadata = saveFile(file, avatarsDirectory,
                FileMetadata.FileCategory.AVATAR, user);

        // Создать thumbnail
        String thumbnailPath = imageProcessingService.createThumbnail(
                file, metadata.getFilePath());
        metadata.setThumbnailPath(thumbnailPath);
        metadata = fileMetadataRepository.save(metadata);

        // Обновить пользователя
        user.setAvatarFileId(metadata.getId());
        userRepository.save(user);

        log.info("✅ Avatar uploaded: {}", metadata.getId());

        return buildUploadResponse(metadata);
    }

    /**
     * Upload документа
     */
    @Transactional
    public FileUploadResponse uploadDocument(MultipartFile file, Long userId) {
        log.info("Uploading document for user: {}", userId);

        // Валидация
        validationService.validateNotEmpty(file);
        validationService.validateSize(file);
        validationService.validateDocumentType(file);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Сохранить документ
        FileMetadata metadata = saveFile(file, documentsDirectory,
                FileMetadata.FileCategory.DOCUMENT, user);

        log.info("✅ Document uploaded: {}", metadata.getId());

        return buildUploadResponse(metadata);
    }

    /**
     * Upload изображения с thumbnail
     */
    @Transactional
    public FileUploadResponse uploadImage(MultipartFile file, Long userId) {
        log.info("Uploading image for user: {}", userId);

        // Валидация
        validationService.validateNotEmpty(file);
        validationService.validateSize(file);
        validationService.validateImageType(file);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Сохранить изображение
        FileMetadata metadata = saveFile(file, imagesDirectory,
                FileMetadata.FileCategory.IMAGE, user);

        // Создать thumbnail
        String thumbnailPath = imageProcessingService.createThumbnail(
                file, metadata.getFilePath());
        metadata.setThumbnailPath(thumbnailPath);
        metadata = fileMetadataRepository.save(metadata);

        log.info("✅ Image uploaded with thumbnail: {}", metadata.getId());

        return buildUploadResponse(metadata);
    }

    /**
     * Upload нескольких изображений
     */
    @Transactional
    public List<FileUploadResponse> uploadMultipleImages(
            List<MultipartFile> files, Long userId) {

        log.info("Uploading {} images for user: {}", files.size(), userId);

        return files.stream()
                .map(file -> uploadImage(file, userId))
                .collect(Collectors.toList());
    }

    /**
     * Общий метод сохранения файла
     */
    private FileMetadata saveFile(MultipartFile file, String directory,
                                  FileMetadata.FileCategory category, User user) {
        try {
            // Генерировать уникальное имя
            String originalFilename = file.getOriginalFilename();
            String sanitizedFilename = validationService.sanitizeFilename(originalFilename);
            String extension = FilenameUtils.getExtension(sanitizedFilename);
            String storedFilename = UUID.randomUUID() + "." + extension;

            // Путь к файлу
            Path filePath = Paths.get(directory, storedFilename);

            // Сохранить файл
            Files.copy(file.getInputStream(), filePath,
                    StandardCopyOption.REPLACE_EXISTING);

            // Создать metadata
            FileMetadata metadata = new FileMetadata();
            metadata.setOriginalFilename(originalFilename);
            metadata.setStoredFilename(storedFilename);
            metadata.setContentType(file.getContentType());
            metadata.setSize(file.getSize());
            metadata.setCategory(category);
            metadata.setFilePath(filePath.toString());
            metadata.setUser(user);

            return fileMetadataRepository.save(metadata);

        } catch (IOException e) {
            log.error("❌ Failed to save file", e);
            throw new FileStorageException("Failed to save file", e);
        }
    }

    /**
     * Получить файл для скачивания
     */
    public Resource loadFileAsResource(Long fileId) {
        try {
            FileMetadata metadata = fileMetadataRepository.findById(fileId)
                    .orElseThrow(() -> new RuntimeException("File not found"));

            Path filePath = Paths.get(metadata.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new FileStorageException("File not found: " + fileId);
            }

        } catch (Exception e) {
            throw new FileStorageException("File not found: " + fileId, e);
        }
    }

    /**
     * Получить thumbnail
     */
    public Resource loadThumbnailAsResource(Long fileId) {
        try {
            FileMetadata metadata = fileMetadataRepository.findById(fileId)
                    .orElseThrow(() -> new RuntimeException("File not found"));

            if (metadata.getThumbnailPath() == null) {
                throw new FileStorageException("Thumbnail not available for this file");
            }

            Path thumbnailPath = Paths.get(metadata.getThumbnailPath());
            Resource resource = new UrlResource(thumbnailPath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new FileStorageException("Thumbnail not found: " + fileId);
            }

        } catch (Exception e) {
            throw new FileStorageException("Thumbnail not found: " + fileId, e);
        }
    }

    /**
     * Список файлов пользователя
     */
    public List<FileInfoResponse> listUserFiles(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<FileMetadata> files = fileMetadataRepository
                .findByUserOrderByUploadedAtDesc(user);

        return files.stream()
                .map(this::buildFileInfoResponse)
                .collect(Collectors.toList());
    }

    /**
     * Удалить файл
     */
    @Transactional
    public void deleteFile(Long fileId) {
        try {
            FileMetadata metadata = fileMetadataRepository.findById(fileId)
                    .orElseThrow(() -> new RuntimeException("File not found"));

            // Удалить файл с диска
            Path filePath = Paths.get(metadata.getFilePath());
            Files.deleteIfExists(filePath);

            // Удалить thumbnail если есть
            if (metadata.getThumbnailPath() != null) {
                Path thumbnailPath = Paths.get(metadata.getThumbnailPath());
                Files.deleteIfExists(thumbnailPath);
            }

            // Удалить metadata из БД
            fileMetadataRepository.delete(metadata);

            log.info("✅ File deleted: {}", fileId);

        } catch (IOException e) {
            log.error("❌ Failed to delete file", e);
            throw new FileStorageException("Failed to delete file", e);
        }
    }

    /**
     * Получить metadata файла
     */
    public FileMetadata getFileMetadata(Long fileId) {
        return fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));
    }

    /**
     * Build upload response
     */
    private FileUploadResponse buildUploadResponse(FileMetadata metadata) {
        String downloadUrl = "/api/files/" + metadata.getId();
        String thumbnailUrl = metadata.getThumbnailPath() != null
                ? "/api/files/" + metadata.getId() + "/thumbnail"
                : null;

        return new FileUploadResponse(
                metadata.getId(),
                metadata.getOriginalFilename(),
                metadata.getContentType(),
                metadata.getSize(),
                metadata.getCategory().name(),
                downloadUrl,
                thumbnailUrl
        );
    }

    /**
     * Build file info response
     */
    private FileInfoResponse buildFileInfoResponse(FileMetadata metadata) {
        String downloadUrl = "/api/files/" + metadata.getId();
        String thumbnailUrl = metadata.getThumbnailPath() != null
                ? "/api/files/" + metadata.getId() + "/thumbnail"
                : null;

        return new FileInfoResponse(
                metadata.getId(),
                metadata.getOriginalFilename(),
                metadata.getContentType(),
                metadata.getSize(),
                metadata.getCategory().name(),
                metadata.getUploadedAt(),
                downloadUrl,
                thumbnailUrl
        );
    }
}
