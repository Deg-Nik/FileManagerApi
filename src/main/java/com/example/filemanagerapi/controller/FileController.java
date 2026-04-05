package com.example.filemanagerapi.controller;

import com.example.filemanagerapi.dto.FileInfoResponse;
import com.example.filemanagerapi.dto.FileUploadResponse;
import com.example.filemanagerapi.dto.ResizeRequest;
import com.example.filemanagerapi.entity.FileMetadata;
import com.example.filemanagerapi.service.FileStorageService;
import com.example.filemanagerapi.service.ImageProcessingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Paths;
import java.util.List;

/**
 * REST Controller для File Manager API
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;
    private final ImageProcessingService imageProcessingService;

    /**
     * Upload аватара
     * В реальном проекте userId берётся из Security Context
     * Здесь для простоты - из header
     */
    @PostMapping("/avatar")
    public ResponseEntity<FileUploadResponse> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("X-User-Id") Long userId) {

        FileUploadResponse response = fileStorageService.uploadAvatar(file, userId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * Upload документа
     */
    @PostMapping("/document")
    public ResponseEntity<FileUploadResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("X-User-Id") Long userId) {

        FileUploadResponse response = fileStorageService.uploadDocument(file, userId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * Upload изображения
     */
    @PostMapping("/image")
    public ResponseEntity<FileUploadResponse> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("X-User-Id") Long userId) {

        FileUploadResponse response = fileStorageService.uploadImage(file, userId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * Upload нескольких изображений
     */
    @PostMapping("/images/multiple")
    public ResponseEntity<List<FileUploadResponse>> uploadMultipleImages(
            @RequestParam("files") List<MultipartFile> files,
            @RequestHeader("X-User-Id") Long userId) {

        List<FileUploadResponse> responses = fileStorageService
                .uploadMultipleImages(files, userId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(responses);
    }

    /**
     * Скачать файл
     */
    @GetMapping("/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {

        FileMetadata metadata = fileStorageService.getFileMetadata(id);
        Resource resource = fileStorageService.loadFileAsResource(id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(metadata.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + metadata.getOriginalFilename() + "\"")
                .body(resource);
    }

    /**
     * Скачать thumbnail
     */
    @GetMapping("/{id}/thumbnail")
    public ResponseEntity<Resource> downloadThumbnail(@PathVariable Long id) {

        Resource resource = fileStorageService.loadThumbnailAsResource(id);

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(resource);
    }

    /**
     * Список файлов пользователя
     */
    @GetMapping("/list")
    public ResponseEntity<List<FileInfoResponse>> listFiles(
            @RequestHeader("X-User-Id") Long userId) {

        List<FileInfoResponse> files = fileStorageService.listUserFiles(userId);

        return ResponseEntity.ok(files);
    }

    /**
     * Удалить файл
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteFile(@PathVariable Long id) {

        fileStorageService.deleteFile(id);

        return ResponseEntity.ok("File deleted successfully");
    }

    /**
     * Resize изображения
     * Создаёт новый resized файл
     */
    @PostMapping("/{id}/resize")
    public ResponseEntity<FileUploadResponse> resizeImage(
            @PathVariable Long id,
            @Valid @RequestBody ResizeRequest request) {

        FileMetadata metadata = fileStorageService.getFileMetadata(id);

        // Проверить что это изображение
        if (metadata.getCategory() != FileMetadata.FileCategory.IMAGE &&
                metadata.getCategory() != FileMetadata.FileCategory.AVATAR) {
            return ResponseEntity
                    .badRequest()
                    .build();
        }

        // Создать resized копию
        String resizedPath = imageProcessingService.createResizedCopy(
                Paths.get(metadata.getFilePath()),
                request.getWidth(),
                request.getHeight(),
                request.getKeepAspectRatio()
        );

        // TODO: Сохранить metadata resized файла в БД

        return ResponseEntity.ok(
                new FileUploadResponse(
                        null,
                        "Resized: " + metadata.getOriginalFilename(),
                        metadata.getContentType(),
                        null,
                        "IMAGE",
                        resizedPath
                )
        );
    }
}
