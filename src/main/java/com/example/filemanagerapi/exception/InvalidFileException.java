package com.example.filemanagerapi.exception;

/**
 * Exception при невалидных файлах
 */
public class InvalidFileException extends RuntimeException {

    public InvalidFileException(String message) {
        super(message);
    }
}