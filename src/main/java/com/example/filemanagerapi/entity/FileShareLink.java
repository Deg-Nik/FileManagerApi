package com.example.filemanagerapi.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "file_share_links")
@Data
@NoArgsConstructor
public class FileShareLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private FileMetadata file;

    @Column(nullable = false, unique = true)
    private String token; // UUID

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime expiresAt; // null = навсегда

    @Column(nullable = false)
    private Boolean active = true;

    @Column(nullable = false)
    private Long downloadCount = 0L;

    @Column(nullable = false)
    private Long viewCount = 0L;

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
}
