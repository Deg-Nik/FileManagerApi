package com.example.filemanagerapi.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request для resize изображения
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResizeRequest {

    @NotNull(message = "Width is required")
    @Min(value = 50, message = "Width must be at least 50px")
    @Max(value = 4000, message = "Width must not exceed 4000px")
    private Integer width;

    @NotNull(message = "Height is required")
    @Min(value = 50, message = "Height must be at least 50px")
    @Max(value = 4000, message = "Height must not exceed 4000px")
    private Integer height;

    private Boolean keepAspectRatio = true;
}