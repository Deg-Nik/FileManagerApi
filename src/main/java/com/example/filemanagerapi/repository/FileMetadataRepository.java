package com.example.filemanagerapi.repository;

import com.example.filemanagerapi.entity.FileMetadata;
import com.example.filemanagerapi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {

    /**
     * Найти все файлы пользователя
     */
    List<FileMetadata> findByUserOrderByUploadedAtDesc(User user);

    /**
     * Найти файлы по категории
     */
    List<FileMetadata> findByUserAndCategory(User user, FileMetadata.FileCategory category);

    /**
     * Найти по stored filename
     */
    Optional<FileMetadata> findByStoredFilename(String storedFilename);

    /**
     * Посчитать общий размер файлов пользователя
     */
    @Query("SELECT SUM(f.size) FROM FileMetadata f WHERE f.user = :user")
    Long sumSizeByUser(User user);
}