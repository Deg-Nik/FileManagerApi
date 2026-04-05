package com.example.filemanagerapi.exception;

/**
 * Exception при ошибках хранения файлов
 */
public class FileStorageException extends RuntimeException {

    public FileStorageException(String message) {
        super(message);
    }

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}