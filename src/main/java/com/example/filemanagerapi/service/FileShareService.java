package com.example.filemanagerapi.service;

import com.example.filemanagerapi.dto.ShareRequest;
import com.example.filemanagerapi.entity.FileMetadata;
import com.example.filemanagerapi.entity.FileShareLink;
import com.example.filemanagerapi.repository.FileMetadataRepository;
import com.example.filemanagerapi.repository.FileShareLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileShareService {

    private final FileShareLinkRepository shareLinkRepository;
    private final FileMetadataRepository fileRepository;

    @Transactional
    public String createShareLink(Long fileId, ShareRequest request) {
        FileMetadata file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("Файл не найден"));

        // Деактивируем существующие ссылки для этого файла
        shareLinkRepository.deactivateAllByFile(file);

        FileShareLink link = new FileShareLink();
        link.setFile(file);
        link.setToken(UUID.randomUUID().toString());
        link.setCreatedAt(LocalDateTime.now());
        link.setExpiresAt(request.getExpiryOption().getExpiryDate());
        link.setActive(true);

        shareLinkRepository.save(link);
        return link.getToken();
    }

    @Transactional
    public FileMetadata accessFileByToken(String token) {
        FileShareLink link = shareLinkRepository.findByTokenAndActiveTrue(token)
                .orElseThrow(() -> new RuntimeException("Ссылка недействительна или не существует"));

        if (link.isExpired()) {
            link.setActive(false);
            throw new RuntimeException("Срок действия ссылки истек");
        }

        link.setViewCount(link.getViewCount() + 1);
        link.setDownloadCount(link.getDownloadCount() + 1);
        return link.getFile();
    }

    @Transactional
    public void revokeShare(Long fileId) {
        FileMetadata file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("Файл не найден"));
        shareLinkRepository.deactivateAllByFile(file);
    }

    public FileShareLink getStats(Long fileId) {
        return shareLinkRepository.findByFileIdAndActiveTrue(fileId)
                .orElseThrow(() -> new RuntimeException("Активная ссылка для файла не найдена"));
    }
}
