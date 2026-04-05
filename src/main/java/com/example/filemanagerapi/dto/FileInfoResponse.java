package com.example.filemanagerapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Информация о файле для списка
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileInfoResponse {

    private Long id;
    private String originalFilename;
    private String contentType;
    private Long size;
    private String category;
    private LocalDateTime uploadedAt;
    private String downloadUrl;
    private String thumbnailUrl;
}