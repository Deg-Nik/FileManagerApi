package com.example.filemanagerapi.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * @author : Nikolai Degtiarev
 * created : 09.04.26
 **/
@Getter // Добавляет getExpiryOption() автоматически
@Setter
public class ShareRequest {

    @NotNull
    private ExpiryOption expiryOption;

    // Если вы не используете Lombok, добавьте вручную:
    // public ExpiryOption getExpiryOption() { return expiryOption; }

    public enum ExpiryOption {
        ONE_DAY(1),
        SEVEN_DAYS(7),
        THIRTY_DAYS(30),
        NEVER(null);

        private final Integer days;

        ExpiryOption(Integer days) {
            this.days = days;
        }

        public LocalDateTime getExpiryDate() {
            return days != null
                    ? LocalDateTime.now().plusDays(days)
                    : null;
        }
    }
}
