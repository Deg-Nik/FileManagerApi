📝 Задание 1: File Sharing with Public URLs
Условие:
Реализовать возможность создания публичных ссылок для файлов.

Требования:

Пользователь может сделать файл публичным
Генерируется уникальная публичная ссылка (UUID token)
Файл доступен по публичной ссылке без авторизации
Можно установить срок действия ссылки (1 день, 7 дней, 30 дней, навсегда)
Можно отозвать публичную ссылку
Статистика: количество просмотров/скачиваний
Endpoints:

POST   /api/files/{id}/share        - Создать публичную ссылку
GET    /api/files/public/{token}    - Скачать файл по публичной ссылке
DELETE /api/files/{id}/share        - Отозвать публичную ссылку
GET    /api/files/{id}/share/stats  - Статистика публичной ссылки
Copy
Что создать:
1. FileShareLink Entity
   @Entity
   @Table(name = "file_share_links")
   public class FileShareLink {

   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Long id;

   @ManyToOne
   @JoinColumn(name = "file_id", nullable = false)
   private FileMetadata file;

   @Column(nullable = false, unique = true)
   private String token;  // UUID

   @Column(nullable = false)
   private LocalDateTime createdAt;

   @Column
   private LocalDateTime expiresAt;  // null = навсегда

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
   Copy
2. ShareRequest DTO
   public class ShareRequest {

   @NotNull
   private ExpiryOption expiryOption;

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
   Copy
3. FileShareService
   @Service
   @RequiredArgsConstructor
   public class FileShareService {

   private final FileShareLinkRepository shareLinkRepository;
   private final FileMetadataRepository fileMetadataRepository;

   /**
    * Создать публичную ссылку
      */
      @Transactional
      public FileShareLink createShareLink(Long fileId, ShareRequest request) {
      FileMetadata file = fileMetadataRepository.findById(fileId)
      .orElseThrow(() -> new RuntimeException("File not found"));

      // Деактивировать старые ссылки
      shareLinkRepository.findByFileAndActiveTrue(file)
      .forEach(link -> {
      link.setActive(false);
      shareLinkRepository.save(link);
      });

      // Создать новую ссылку
      FileShareLink shareLink = new FileShareLink();
      shareLink.setFile(file);
      shareLink.setToken(UUID.randomUUID().toString());
      shareLink.setExpiresAt(request.getExpiryOption().getExpiryDate());

      return shareLinkRepository.save(shareLink);
      }

   /**
    * Получить файл по публичной ссылке
      */
      @Transactional
      public FileMetadata getFileByToken(String token) {
      FileShareLink shareLink = shareLinkRepository.findByToken(token)
      .orElseThrow(() -> new RuntimeException("Share link not found"));

      // Проверки
      if (!shareLink.getActive()) {
      throw new RuntimeException("Share link is inactive");
      }

      if (shareLink.isExpired()) {
      throw new RuntimeException("Share link has expired");
      }

      // Увеличить счётчик
      shareLink.setDownloadCount(shareLink.getDownloadCount() + 1);
      shareLinkRepository.save(shareLink);

      return shareLink.getFile();
      }
      }
      Copy
      Тестирование:

1. POST /api/files/1/share (expiryOption: SEVEN_DAYS)
2. Response: { token: "abc-123-xyz", expiresAt: "2025-04-05T10:00:00" }
3. GET /api/files/public/abc-123-xyz (работает без авторизации)
4. DELETE /api/files/1/share (отзыв ссылки)
5. GET /api/files/public/abc-123-xyz (404 или 403)