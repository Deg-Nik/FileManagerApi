package com.example.filemanagerapi.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Метаданные файла в БД
 * Сам файл хранится на диске
 */
@Entity
@Table(name = "file_metadata")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Оригинальное имя файла
    @Column(nullable = false)
    private String originalFilename;

    // Имя файла на диске (UUID + extension)
    @Column(nullable = false, unique = true)
    private String storedFilename;

    // MIME type
    @Column(nullable = false)
    private String contentType;

    // Размер в байтах
    @Column(nullable = false)
    private Long size;

    // Категория файла
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private FileCategory category;

    // Путь к файлу на диске
    @Column(nullable = false)
    private String filePath;

    // Путь к thumbnail (если изображение)
    @Column
    private String thumbnailPath;

    // Владелец файла
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Дата загрузки
    @Column(nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
    }

    /**
     * Категории файлов
     */
    public enum FileCategory {
        AVATAR,      // Аватары пользователей
        DOCUMENT,    // Документы (PDF, DOCX, XLSX)
        IMAGE,       // Обычные изображения
        OTHER        // Прочее
    }
}