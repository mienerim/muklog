package com.nomlog.backend.record;

import java.time.LocalDateTime;

public record RecordUpdateRequest(
    String foodName,
    String category,
    String imageUrl,
    LocalDateTime eatenAt
) {
}
