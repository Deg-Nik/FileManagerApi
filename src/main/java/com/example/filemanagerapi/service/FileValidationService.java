package com.example.filemanagerapi.service;

import com.example.filemanagerapi.exception.InvalidFileException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Сервис для валидации файлов
 */
@Service
public class FileValidationService {
    // Максимальный размер файла (10 MB)
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    // Разрешенные типы изображений
    private static final List<String> ALLOWED_IMAGE_TYPES =
            Arrays.asList("image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp");

    // Разрешенные типы документов
    private static final List<String> ALLOWED_DOCUMENT_TYPES = Arrays.asList(
            "application/pdf",                                                          // PDF
            "application/msword",                                                       // DOC
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",  // DOCX
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",        // XLSX
            "application/vnd.ms-excel",                                                 // XLS
            "text/plain"                                                                // TXT
    );

    /**
     * Проверить, что файл не пустой
     */
    public void validateNotEmpty(MultipartFile file) {
        if (file == null || file.isEmpty())
            throw new InvalidFileException("File is empty");
    }

    /**
     * Проверить размер файла
     */
    public void validateSize(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE)
            throw new InvalidFileException(
                    String.format("File size (%d bytes) exceeds maximum allowed size (%d bytes)",
                            file.getSize(), MAX_FILE_SIZE)
            );
    }

    /**
     * Проверить что это изображение
     */
    public void validateImageType(MultipartFile file) {
        String contentType = file.getContentType();

        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new InvalidFileException(
                    "Invalid image type. Allowed: JPEG, PNG, GIF, WEBP"
            );
        }
    }

    /**
     * Проверить что это документ
     */
    public void validateDocumentType(MultipartFile file) {
        String contentType = file.getContentType();

        if (contentType == null || !ALLOWED_DOCUMENT_TYPES.contains(contentType)) {
            throw new InvalidFileException(
                    "Invalid document type. Allowed: PDF, DOCX, XLSX, DOC, XLS, TXT"
            );
        }
    }

    /**
     * Проверить расширение файла
     */
    public void validateExtension(String filename, List<String> allowedExtensions) {
        String extension = FilenameUtils.getExtension(filename);

        if (extension == null || !allowedExtensions.contains(extension))
            throw new InvalidFileException(
                    "Invalid file extension. Allowed: " + String.join(", ", allowedExtensions)
            );
    }


    // .jpg (FF D8 FF), .pdf(25 50 44 46 - %PDF)
    /**
     * Валидация Magic Bytes (первые байты файла)
     * Защита от переименования (.exe -> .jpg)
     */
    public void validateImageMagicBytes(MultipartFile file) throws IOException {
        byte[] fileBytes = new byte[10];
        int bytesRead = file.getInputStream().read(fileBytes);

        if (bytesRead < 2) {
            throw new InvalidFileException("Invalid image file");
        }

        // JPEG magic bytes: FF D8 FF
        if (fileBytes[0] == (byte) 0xFF &&
                fileBytes[1] == (byte) 0xD8 &&
                fileBytes[2] == (byte) 0xFF) {
            return; // Valid JPEG
        }

        // PNG magic bytes: 89 50 4E 47
        if (fileBytes[0] == (byte) 0x89 &&
                fileBytes[1] == (byte) 0x50 &&
                fileBytes[2] == (byte) 0x4E &&
                fileBytes[3] == (byte) 0x47) {
            return; // Valid PNG
        }

        // GIF magic bytes: 47 49 46
        if (fileBytes[0] == (byte) 0x47 &&
                fileBytes[1] == (byte) 0x49 &&
                fileBytes[2] == (byte) 0x46) {
            return; // Valid GIF
        }

        throw new InvalidFileException("Invalid image file content");
    }

    /**
     * Удаление опасных символов
     */
    public String sanitizeFilename(String filename) {
        // Удалить path traversal
        filename = filename.replaceAll("\\.\\./", "");
        filename = filename.replaceAll("\\.\\\\", "");

        // Оставить только безопасные символы
        filename = filename.replaceAll("[^a-zA-Z0-9._-]", "_");

        // Ограничить длину
        if (filename.length() > 255) {
            String extension = FilenameUtils.getExtension(filename);
            String name = FilenameUtils.getBaseName(filename).substring(0, 250);
            filename = name + "." + extension;
        }

        return filename;
    }
}
