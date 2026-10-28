package dev.studio.atelier;

import java.time.LocalDateTime;

public final class Models {
  private Models() {}
  public record Template(String id, String title, String category, String description, String coverUrl,
                         int requiredImageCount, String acceptedMimeTypes, long maxFileSizeBytes) {}
  public record Task(String id, String ownerId, String templateId, String status, String currentStage,
                     String failureReason, String resultFileId, String failAtStage,
                     LocalDateTime createdAt, LocalDateTime updatedAt) {}
  public record StoredFile(String id, String taskId, String kind, String originalName, String mimeType,
                           String storageName, LocalDateTime createdAt) {}
}
