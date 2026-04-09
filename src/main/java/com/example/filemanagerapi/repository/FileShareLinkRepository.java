package com.example.filemanagerapi.repository;

import com.example.filemanagerapi.entity.FileMetadata;
import com.example.filemanagerapi.entity.FileShareLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface FileShareLinkRepository extends JpaRepository<FileShareLink, Long> {

    Optional<FileShareLink> findByTokenAndActiveTrue(String token);

    List<FileShareLink> findByFileAndActiveTrue(FileMetadata file);

    Optional<FileShareLink> findByFileIdAndActiveTrue(Long fileId);

    @Modifying
    @Query("UPDATE FileShareLink f SET f.active = false WHERE f.file = :file AND f.active = true")
    void deactivateAllByFile(FileMetadata file);
}
