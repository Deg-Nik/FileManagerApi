# 📁 Лекция 19: File Uploading - Конспект для студентов

**Spring Boot File Upload Integration**

⏱️ **Продолжительность**: 3-3.5 часа  
🎯 **Цель**: Научиться загружать, обрабатывать и безопасно хранить файлы  
📚 **Уровень**: Средний-Продвинутый

---

## 📋 План лекции

1. MultipartFile - основы загрузки
2. Конфигурация загрузки файлов
3. Хранение файлов (Local, Database, S3)
4. Обработка изображений (Thumbnailator)
5. Валидация и безопасность (Magic Bytes)
6. Скачивание файлов
7. Best Practices
8. Troubleshooting

---

## 🎯 Что мы научимся делать

К концу лекции вы сможете:

✅ Загружать файлы через REST API  
✅ Сохранять файлы на диск и в БД  
✅ Создавать thumbnails для изображений  
✅ Валидировать файлы по типу и размеру  
✅ Проверять файлы через Magic Bytes  
✅ Организовывать файлы по категориям  
✅ Скачивать файлы через API  
✅ Интегрировать с AWS S3  

---

## 📦 Часть 1: Настройка проекта (10 минут)

### 1.1 Dependencies

Добавить в `pom.xml`:

```xml
<!-- Image Processing -->
<dependency>
    <groupId>net.coobird</groupId>
    <artifactId>thumbnailator</artifactId>
    <version>0.4.19</version>
</dependency>

<!-- File Utilities -->
<dependency>
    <groupId>commons-io</groupId>
    <artifactId>commons-io</artifactId>
    <version>2.11.0</version>
</dependency>
```

### 1.2 Конфигурация в application.properties

```properties
# File Upload Configuration
spring.servlet.multipart.enabled=true
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=50MB
spring.servlet.multipart.location=/tmp

# Custom properties
file.upload.dir=uploads
file.upload.avatars-dir=${file.upload.dir}/avatars
file.upload.documents-dir=${file.upload.dir}/documents
file.upload.images-dir=${file.upload.dir}/images
```

**Что это значит:**

- `max-file-size=10MB` - максимальный размер одного файла
- `max-request-size=50MB` - максимальный размер всего запроса (несколько файлов)
- `enabled=true` - включить multipart поддержку
- `location=/tmp` - временная папка для больших файлов

---

## 📤 Часть 2: MultipartFile - Основы (20 минут)

### 2.1 Что такое MultipartFile?

**MultipartFile** - это Spring интерфейс для работы с загруженными файлами.

**Основные методы:**

```java
String getOriginalFilename()  // Имя файла (photo.jpg)
long getSize()                // Размер в байтах
String getContentType()       // MIME тип (image/jpeg)
byte[] getBytes()             // Содержимое файла
InputStream getInputStream()  // Поток для чтения
void transferTo(File dest)    // Сохранить на диск
boolean isEmpty()             // Проверка пустоты
```

### 2.2 Простой endpoint для загрузки

**FileController.java:**

```java
package com.example.filemanager.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.*;

@RestController
@RequestMapping("/api/files")
public class FileController {
    
    private final Path uploadPath = Paths.get("uploads");
    
    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(
            @RequestParam("file") MultipartFile file) {
        
        try {
            // Создать папку если нет
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            // Получить информацию о файле
            String filename = file.getOriginalFilename();
            long size = file.getSize();
            String type = file.getContentType();
            
            // Сохранить файл
            Path filepath = uploadPath.resolve(filename);
            file.transferTo(filepath.toFile());
            
            return ResponseEntity.ok(
                String.format("Uploaded: %s (%d bytes, %s)", 
                             filename, size, type)
            );
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("Upload failed: " + e.getMessage());
        }
    }
}
```

### 2.3 Тестирование в Postman

**Request:**
```
Method: POST
URL: http://localhost:8080/api/files/upload

Headers: (автоматически)
Content-Type: multipart/form-data

Body: form-data
  KEY:   file
  VALUE: [Select Files] → photo.jpg
  TYPE:  File  ← ВАЖНО!
```

**Response:**
```
Uploaded: photo.jpg (2048576 bytes, image/jpeg)
```

---

## 🗂️ Часть 3: FileStorageService (30 минут)

### 3.1 Зачем Service слой?

**Проблемы без Service:**
- Логика в контроллере (плохо для тестирования)
- Дублирование кода
- Сложно переключить хранилище (Local → S3)

**Решение:** Создать `FileStorageService`

### 3.2 Создать FileStorageService

```java
package com.example.filemanager.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {
    
    @Value("${file.upload.dir}")
    private String uploadDir;
    
    private Path uploadPath;
    
    @PostConstruct
    public void init() {
        this.uploadPath = Paths.get(uploadDir);
        try {
            Files.createDirectories(uploadPath);
            log.info("✅ Upload directory created: {}", uploadPath);
        } catch (IOException e) {
            log.error("❌ Failed to create upload directory", e);
            throw new RuntimeException("Could not create upload directory", e);
        }
    }
    
    /**
     * Сохранить файл с уникальным именем
     */
    public String saveFile(MultipartFile file) throws IOException {
        // Генерировать уникальное имя
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String uniqueFilename = UUID.randomUUID() + extension;
        
        // Сохранить файл
        Path targetPath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), targetPath, 
                   StandardCopyOption.REPLACE_EXISTING);
        
        log.info("✅ File saved: {}", uniqueFilename);
        return uniqueFilename;
    }
    
    /**
     * Получить расширение файла
     */
    private String getFileExtension(String filename) {
        if (filename == null) return "";
        int lastDot = filename.lastIndexOf('.');
        return (lastDot == -1) ? "" : filename.substring(lastDot);
    }
}
```

**Ключевые моменты:**

- `@PostConstruct` - выполняется после создания bean (создаёт папку)
- `UUID.randomUUID()` - уникальное имя для каждого файла
- `StandardCopyOption.REPLACE_EXISTING` - перезаписать если существует
- Логирование успеха/ошибок

### 3.3 Использовать в контроллере

```java
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {
    
    private final FileStorageService fileStorageService;
    
    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(
            @RequestParam("file") MultipartFile file) {
        
        try {
            String filename = fileStorageService.saveFile(file);
            return ResponseEntity.ok("File saved as: " + filename);
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                .body("Upload failed: " + e.getMessage());
        }
    }
}
```

---

## 📁 Часть 4: Организация файлов (25 минут)

### 4.1 FileCategory Enum

Организовать файлы по категориям:

```java
package com.example.filemanager.model;

public enum FileCategory {
    AVATARS("avatars"),
    DOCUMENTS("documents"),
    IMAGES("images"),
    VIDEOS("videos"),
    ATTACHMENTS("attachments");
    
    private final String directory;
    
    FileCategory(String directory) {
        this.directory = directory;
    }
    
    public String getDirectory() {
        return directory;
    }
}
```

### 4.2 Обновить FileStorageService

```java
@Service
@Slf4j
public class FileStorageService {
    
    @Value("${file.upload.dir}")
    private String uploadDir;
    
    private Path rootPath;
    
    @PostConstruct
    public void init() {
        this.rootPath = Paths.get(uploadDir);
        try {
            Files.createDirectories(rootPath);
            
            // Создать подпапки для категорий
            for (FileCategory category : FileCategory.values()) {
                Path categoryPath = rootPath.resolve(category.getDirectory());
                Files.createDirectories(categoryPath);
            }
            
            log.info("✅ All directories created");
        } catch (IOException e) {
            throw new RuntimeException("Could not create directories", e);
        }
    }
    
    /**
     * Сохранить файл в категорию
     */
    public String saveFile(MultipartFile file, FileCategory category) 
            throws IOException {
        
        // Путь к категории
        Path categoryPath = rootPath.resolve(category.getDirectory());
        
        // Уникальное имя
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String uniqueFilename = UUID.randomUUID() + extension;
        
        // Полный путь
        Path targetPath = categoryPath.resolve(uniqueFilename);
        
        // Сохранить
        Files.copy(file.getInputStream(), targetPath,
                   StandardCopyOption.REPLACE_EXISTING);
        
        // Вернуть относительный путь
        return category.getDirectory() + "/" + uniqueFilename;
    }
    
    private String getFileExtension(String filename) {
        if (filename == null) return "";
        int lastDot = filename.lastIndexOf('.');
        return (lastDot == -1) ? "" : filename.substring(lastDot);
    }
}
```

### 4.3 Структура папок

После запуска:

```
uploads/
├── avatars/
│   ├── a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg
│   └── b2c3d4e5-f6a7-8901-bcde-f12345678901.jpg
│
├── documents/
│   ├── c3d4e5f6-a7b8-9012-cdef-123456789012.pdf
│   └── d4e5f6a7-b8c9-0123-def1-234567890123.docx
│
└── images/
    ├── e5f6a7b8-c9d0-1234-ef12-345678901234.png
    └── f6a7b8c9-d0e1-2345-f123-456789012345.jpg
```

---

## 🗄️ Часть 5: Метаданные в БД (20 минут)

### 5.1 FileMetadata Entity

```java
package com.example.filemanager.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

@Entity
@Table(name = "file_metadata")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class FileMetadata {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String originalName;      // photo.jpg
    
    @Column(nullable = false, unique = true)
    private String storedName;        // uuid.jpg
    
    @Column(nullable = false)
    private String storedPath;        // avatars/uuid.jpg
    
    private String contentType;       // image/jpeg
    
    private Long sizeInBytes;         // 2048576
    
    @Enumerated(EnumType.STRING)
    private FileCategory category;    // AVATARS
    
    @CreatedDate
    private LocalDateTime uploadedAt;
    
    @Column(name = "uploaded_by")
    private Long uploadedBy;          // User ID (опционально)
}
```

### 5.2 FileMetadataRepository

```java
package com.example.filemanager.repository;

import com.example.filemanager.entity.FileMetadata;
import com.example.filemanager.model.FileCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {
    
    Optional<FileMetadata> findByStoredName(String storedName);
    
    List<FileMetadata> findByCategory(FileCategory category);
    
    List<FileMetadata> findByUploadedBy(Long userId);
}
```

### 5.3 Сохранение метаданных

Обновить `FileStorageService`:

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {
    
    private final FileMetadataRepository metadataRepository;
    
    @Value("${file.upload.dir}")
    private String uploadDir;
    
    private Path rootPath;
    
    // ... init() method ...
    
    /**
     * Сохранить файл с метаданными
     */
    public FileMetadata saveFileWithMetadata(MultipartFile file, 
                                            FileCategory category,
                                            Long uploadedBy) 
            throws IOException {
        
        // Сохранить файл на диск
        String storedPath = saveFileToDisk(file, category);
        String storedName = Paths.get(storedPath).getFileName().toString();
        
        // Создать metadata
        FileMetadata metadata = new FileMetadata();
        metadata.setOriginalName(file.getOriginalFilename());
        metadata.setStoredName(storedName);
        metadata.setStoredPath(storedPath);
        metadata.setContentType(file.getContentType());
        metadata.setSizeInBytes(file.getSize());
        metadata.setCategory(category);
        metadata.setUploadedBy(uploadedBy);
        
        // Сохранить в БД
        return metadataRepository.save(metadata);
    }
    
    private String saveFileToDisk(MultipartFile file, FileCategory category) 
            throws IOException {
        Path categoryPath = rootPath.resolve(category.getDirectory());
        String uniqueFilename = UUID.randomUUID() + 
                               getFileExtension(file.getOriginalFilename());
        Path targetPath = categoryPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), targetPath,
                   StandardCopyOption.REPLACE_EXISTING);
        return category.getDirectory() + "/" + uniqueFilename;
    }
}
```

---

## 🖼️ Часть 6: Обработка изображений (30 минут)

### 6.1 Зачем обрабатывать изображения?

**Проблема:**
```
Пользователь загружает фото 5000x4000 px (8 MB)
→ Медленная загрузка страницы
→ Расход трафика
→ Плохой UX
```

**Решение:**
```
1. Сохранить оригинал
2. Создать thumbnail 200x200 px (50 KB)
3. Показывать thumbnail в списке
4. Оригинал только при клике
```

### 6.2 ImageProcessingService

```java
package com.example.filemanager.service;

import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.Path;

@Service
@Slf4j
public class ImageProcessingService {
    
    /**
     * Создать thumbnail изображения
     */
    public void createThumbnail(Path originalPath, 
                               Path thumbnailPath,
                               int width, 
                               int height) throws IOException {
        
        log.info("Creating thumbnail: {} -> {}", 
                originalPath.getFileName(), 
                thumbnailPath.getFileName());
        
        Thumbnails.of(originalPath.toFile())
            .size(width, height)
            .outputQuality(0.8)  // 80% качества
            .toFile(thumbnailPath.toFile());
        
        log.info("✅ Thumbnail created");
    }
    
    /**
     * Изменить размер изображения
     */
    public void resizeImage(Path originalPath,
                           Path resizedPath,
                           int maxWidth,
                           int maxHeight) throws IOException {
        
        Thumbnails.of(originalPath.toFile())
            .size(maxWidth, maxHeight)
            .outputQuality(0.9)
            .toFile(resizedPath.toFile());
    }
}
```

### 6.3 Upload изображения с thumbnail

```java
@PostMapping("/upload-image")
public ResponseEntity<FileResponse> uploadImage(
        @RequestParam("file") MultipartFile file,
        @RequestHeader("X-User-Id") Long userId) {
    
    try {
        // 1. Сохранить оригинал
        FileMetadata metadata = fileStorageService
            .saveFileWithMetadata(file, FileCategory.IMAGES, userId);
        
        // 2. Создать thumbnail
        Path originalPath = Paths.get("uploads", metadata.getStoredPath());
        String thumbnailName = "thumb_" + metadata.getStoredName();
        Path thumbnailPath = originalPath.getParent().resolve(thumbnailName);
        
        imageProcessingService.createThumbnail(
            originalPath, thumbnailPath, 200, 200
        );
        
        // 3. Сохранить путь к thumbnail
        metadata.setThumbnailPath(
            metadata.getCategory().getDirectory() + "/" + thumbnailName
        );
        metadataRepository.save(metadata);
        
        return ResponseEntity.ok(new FileResponse(metadata));
        
    } catch (IOException e) {
        log.error("Failed to upload image", e);
        return ResponseEntity.internalServerError().build();
    }
}
```

---

## 🔒 Часть 7: Валидация и безопасность (40 минут)

### 7.1 Три уровня валидации

**Level 1:** Размер файла  
**Level 2:** Content-Type (MIME type)  
**Level 3:** Magic Bytes (реальное содержимое)

### 7.2 FileValidationService

```java
package com.example.filemanager.service;

import com.example.filemanager.exception.FileSizeException;
import com.example.filemanager.exception.InvalidFileTypeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class FileValidationService {
    
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB
    
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
        "image/jpeg",
        "image/png",
        "image/gif",
        "image/webp"
    );
    
    private static final List<String> ALLOWED_DOCUMENT_TYPES = Arrays.asList(
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );
    
    /**
     * Level 1: Проверить размер файла
     */
    public void validateFileSize(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileSizeException(
                String.format("File size %d exceeds maximum %d", 
                             file.getSize(), MAX_FILE_SIZE)
            );
        }
    }
    
    /**
     * Level 2: Проверить Content-Type
     */
    public void validateImageType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new InvalidFileTypeException(
                "Invalid image type: " + contentType
            );
        }
    }
    
    /**
     * Level 3: Проверить Magic Bytes
     */
    public void validateImageMagicBytes(MultipartFile file) throws IOException {
        byte[] fileBytes = file.getBytes();
        
        if (fileBytes.length < 4) {
            throw new InvalidFileTypeException("File too small");
        }
        
        // JPEG: FF D8 FF
        if (fileBytes[0] == (byte) 0xFF &&
            fileBytes[1] == (byte) 0xD8 &&
            fileBytes[2] == (byte) 0xFF) {
            return; // Valid JPEG
        }
        
        // PNG: 89 50 4E 47
        if (fileBytes[0] == (byte) 0x89 &&
            fileBytes[1] == (byte) 0x50 &&
            fileBytes[2] == (byte) 0x4E &&
            fileBytes[3] == (byte) 0x47) {
            return; // Valid PNG
        }
        
        // GIF: 47 49 46 38
        if (fileBytes[0] == (byte) 0x47 &&
            fileBytes[1] == (byte) 0x49 &&
            fileBytes[2] == (byte) 0x46 &&
            fileBytes[3] == (byte) 0x38) {
            return; // Valid GIF
        }
        
        throw new InvalidFileTypeException(
            "File is not a valid image (magic bytes check failed)"
        );
    }
    
    /**
     * Полная валидация изображения
     */
    public void validateImage(MultipartFile file) throws IOException {
        validateFileSize(file);
        validateImageType(file);
        validateImageMagicBytes(file);
    }
}
```

### 7.3 Magic Bytes - Почему важно?

**Атака без проверки:**
```
1. Хакер создаёт: virus.exe
2. Переименовывает в: cute-cat.jpg
3. Content-Type: image/jpeg (легко подделать!)
4. Загружает на сервер
5. Сервер выполняет: virus.exe
6. 💥 Сервер скомпрометирован!
```

**Защита с Magic Bytes:**
```
1. Хакер загружает: cute-cat.jpg (virus.exe)
2. Сервер читает первые байты: 4D 5A 90 00 (EXE signature)
3. Magic Bytes НЕ совпадают с JPEG (FF D8 FF)
4. ❌ Файл отклонён!
5. ✅ Сервер защищён!
```

### 7.4 Использование валидации

```java
@PostMapping("/upload-image")
public ResponseEntity<FileResponse> uploadImage(
        @RequestParam("file") MultipartFile file,
        @RequestHeader("X-User-Id") Long userId) {
    
    try {
        // Валидация ПЕРЕД сохранением!
        validationService.validateImage(file);
        
        // Теперь безопасно сохранять
        FileMetadata metadata = fileStorageService
            .saveFileWithMetadata(file, FileCategory.IMAGES, userId);
        
        return ResponseEntity.ok(new FileResponse(metadata));
        
    } catch (FileSizeException | InvalidFileTypeException e) {
        log.warn("Validation failed: {}", e.getMessage());
        return ResponseEntity.badRequest()
            .body(new FileResponse(e.getMessage()));
    } catch (IOException e) {
        log.error("Upload failed", e);
        return ResponseEntity.internalServerError().build();
    }
}
```

---

## 📥 Часть 8: Скачивание файлов (20 минут)

### 8.1 Download endpoint

```java
@GetMapping("/download/{id}")
public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
    
    try {
        // Получить метаданные
        FileMetadata metadata = metadataRepository.findById(id)
            .orElseThrow(() -> new FileNotFoundException("File not found"));
        
        // Загрузить файл как Resource
        Path filePath = Paths.get("uploads", metadata.getStoredPath());
        Resource resource = new UrlResource(filePath.toUri());
        
        if (!resource.exists()) {
            throw new FileNotFoundException("File not found on disk");
        }
        
        // Вернуть файл
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(metadata.getContentType()))
            .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + 
                    metadata.getOriginalName() + "\"")
            .body(resource);
            
    } catch (Exception e) {
        log.error("Download failed", e);
        return ResponseEntity.notFound().build();
    }
}
```

**Тестирование:**
```
GET http://localhost:8080/api/files/download/1

Response: File download starts!
```

### 8.2 Streaming для больших файлов

Для файлов >100MB используй streaming:

```java
@GetMapping("/stream/{id}")
public ResponseEntity<StreamingResponseBody> streamFile(@PathVariable Long id) {
    
    FileMetadata metadata = metadataRepository.findById(id)
        .orElseThrow(() -> new FileNotFoundException());
    
    Path filePath = Paths.get("uploads", metadata.getStoredPath());
    
    StreamingResponseBody stream = outputStream -> {
        Files.copy(filePath, outputStream);
    };
    
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + metadata.getOriginalName() + "\"")
        .body(stream);
}
```

---

## ☁️ Часть 9: AWS S3 Integration (Бонус, 15 минут)

### 9.1 Когда использовать S3?

**Local storage хорош для:**
- Development
- Небольших приложений (<1000 файлов/день)
- Single server

**S3 нужен для:**
- Production приложений
- Масштабирование (миллионы файлов)
- Multiple servers
- CDN интеграция
- Географическая распределённость

### 9.2 S3 Dependency

```xml
<dependency>
    <groupId>com.amazonaws</groupId>
    <artifactId>aws-java-sdk-s3</artifactId>
    <version>1.12.500</version>
</dependency>
```

### 9.3 S3StorageService (пример)

```java
@Service
@RequiredArgsConstructor
public class S3StorageService {
    
    @Autowired
    private AmazonS3 s3Client;
    
    @Value("${aws.s3.bucket}")
    private String bucketName;
    
    public String uploadToS3(MultipartFile file) throws IOException {
        String key = UUID.randomUUID() + "_" + file.getOriginalFilename();
        
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(file.getContentType());
        metadata.setContentLength(file.getSize());
        
        s3Client.putObject(
            bucketName,
            key,
            file.getInputStream(),
            metadata
        );
        
        return s3Client.getUrl(bucketName, key).toString();
    }
}
```

---

## 📋 Часть 10: Best Practices (15 минут)

### 10.1 Security Checklist

⚠️ **ОБЯЗАТЕЛЬНО:**

✅ Валидировать размер файла  
✅ Проверять Content-Type  
✅ Проверять Magic Bytes  
✅ Использовать UUID для имён  
✅ Хранить вне web root  
✅ Sanitize filenames  
✅ Rate limiting  
✅ Антивирус сканирование (production)  

### 10.2 Storage Best Practices

✅ Организовать файлы по категориям  
✅ Хранить метаданные в БД  
✅ Создавать thumbnails для изображений  
✅ Использовать cloud storage (S3) для production  
✅ Настроить backup  
✅ Мониторить размер хранилища  

### 10.3 Performance Best Practices

✅ Async обработка для больших файлов  
✅ Streaming для download >10MB  
✅ CDN для статических файлов  
✅ Compression для изображений  
✅ Lazy loading в UI  

---

## ❌ Часть 11: Troubleshooting (10 минут)

### Ошибка 1: MaxUploadSizeExceededException

**Симптом:**
```
org.springframework.web.multipart.MaxUploadSizeExceededException
```

**Решение:**
```properties
spring.servlet.multipart.max-file-size=50MB
spring.servlet.multipart.max-request-size=100MB
```

### Ошибка 2: Current request is not a multipart request

**Симптом:**
```
MultipartException: Current request is not a multipart request
```

**Решение:**
В Postman:
- Body → **form-data** (НЕ raw!)
- KEY = `file`, TYPE = **File**

### Ошибка 3: FileNotFoundException при download

**Симптом:**
```
File not found: uploads/avatars/uuid.jpg
```

**Решение:**
1. Проверить что файл существует: `ls uploads/avatars/`
2. Проверить путь в БД
3. Проверить permissions

### Ошибка 4: OutOfMemoryError

**Симптом:**
```
java.lang.OutOfMemoryError: Java heap space
```

**Решение:**
Использовать streaming для больших файлов:
```java
StreamingResponseBody stream = outputStream -> {
    Files.copy(filePath, outputStream);
};
```

---

## 📚 Резюме лекции

### Что узнали:

✅ MultipartFile для загрузки файлов  
✅ Конфигурация Spring multipart  
✅ FileStorageService для организации  
✅ Хранение метаданных в БД  
✅ Image processing с Thumbnailator  
✅ 3 уровня валидации (Size, Type, Magic Bytes)  
✅ Безопасная загрузка файлов  
✅ Download и streaming  
✅ AWS S3 интеграция  

### Код который написали:

```
FileController.java           - REST endpoints
FileStorageService.java       - Хранение файлов
ImageProcessingService.java   - Обработка изображений
FileValidationService.java    - Валидация
FileMetadata.java            - Entity
FileMetadataRepository.java  - Repository
FileCategory.java            - Enum
```

### Endpoints которые создали:

```
POST   /api/files/upload        - Загрузка файла
POST   /api/files/upload-image  - Загрузка изображения с thumbnail
POST   /api/files/upload-batch  - Множественная загрузка
GET    /api/files/download/{id} - Скачивание файла
GET    /api/files/stream/{id}   - Streaming файла
GET    /api/files/list          - Список файлов
DELETE /api/files/{id}          - Удаление файла
```

---

## 📖 Домашнее задание

📋 См. файл: `lecture-19-homework.md`

**4 задачи:**
1. File Sharing (публичные ссылки)
2. Video upload с thumbnails
3. AWS S3 интеграция
4. File versioning

---

## 🔗 Дополнительные материалы

- `lecture-19-mini-project-file-manager.md` - Полный код проекта
- `lecture-19-homework-solutions.md` - Решения ДЗ
- `blog-platform-file-integration.md` - Интеграция в Blog Platform
- `Postman Collection` - Тестирование API

---

## 💡 Советы для запоминания

1. **MultipartFile** - Spring интерфейс для файлов
2. **UUID** - уникальные имена файлов
3. **Magic Bytes** - проверка реального типа файла
4. **Thumbnailator** - библиотека для изображений
5. **StreamingResponseBody** - для больших файлов
6. **S3** - cloud storage для production
7. **3 уровня валидации** - Size → Type → Magic Bytes

---

## ❓ Вопросы для самопроверки

1. Что такое MultipartFile и какие у него методы?
2. Зачем использовать UUID для имён файлов?
3. Что такое Magic Bytes и почему они важны?
4. Как создать thumbnail изображения?
5. В чём разница между Local storage и S3?
6. Как скачать файл через REST API?
7. Что такое StreamingResponseBody и когда его использовать?

---

**Конспект Лекции 19: File Uploading**  
Версия: 1.0  
Дата: 2025-03-31

🎓 Успешной учёбы!
