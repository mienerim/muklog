package com.nomlog.backend.record;

import java.time.LocalDateTime;

public record RecordResponse(
    Long id,
    String foodName,
    String category,
    String imageUrl,
    LocalDateTime eatenAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

    public static RecordResponse from(Record record) {
        return new RecordResponse(
            record.getId(),
            record.getFoodName(),
            record.getCategory(),
            record.getImageUrl(),
            record.getEatenAt(),
            record.getCreatedAt(),
            record.getUpdatedAt()
        );
    }
}
