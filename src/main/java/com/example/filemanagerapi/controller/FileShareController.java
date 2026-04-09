package com.example.filemanagerapi.controller;

import com.example.filemanagerapi.dto.ShareRequest;
import com.example.filemanagerapi.entity.FileMetadata;
import com.example.filemanagerapi.entity.FileShareLink;
import com.example.filemanagerapi.service.FileShareService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileShareController {

    private final FileShareService shareService;

    @PostMapping("/{id}/share")
    public ResponseEntity<String> createShare(@PathVariable Long id, @RequestBody ShareRequest request) {
        String token = shareService.createShareLink(id, request);
        return ResponseEntity.ok("/api/files/public/" + token);
    }

    @GetMapping("/public/{token}")
    public ResponseEntity<Resource> downloadPublic(@PathVariable String token) {
        FileMetadata file = shareService.accessFileByToken(token);
        Resource resource = new FileSystemResource(file.getFilePath());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getOriginalFilename() + "\"")
                .body(resource);
    }

    @DeleteMapping("/{id}/share")
    public ResponseEntity<Void> revokeShare(@PathVariable Long id) {
        shareService.revokeShare(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/share/stats")
    public ResponseEntity<?> getStats(@PathVariable Long id) {
        FileShareLink link = shareService.getStats(id);
        return ResponseEntity.ok().body(new Object() {
            public final Long views = link.getViewCount();
            public final Long downloads = link.getDownloadCount();
            public final boolean active = link.getActive();
            public final String expiresAt = String.valueOf(link.getExpiresAt());
        });
    }
}
