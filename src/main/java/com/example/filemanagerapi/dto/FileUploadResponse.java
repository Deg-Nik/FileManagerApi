package com.example.filemanagerapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response после успешной загрузки файла
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponse {

    private Long id;
    private String filename;
    private String contentType;
    private Long size;
    private String category;
    private String downloadUrl;
    private String thumbnailUrl;  // Если изображение

    public FileUploadResponse(Long id, String filename, String contentType,
                              Long size, String category, String downloadUrl) {
        this.id = id;
        this.filename = filename;
        this.contentType = contentType;
        this.size = size;
        this.category = category;
        this.downloadUrl = downloadUrl;
    }

}