package com.muklog.backend.record;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public record RecordCreateRequest(
    @NotBlank String foodName,
    String category,
    String imageUrl,
    LocalDateTime eatenAt
) {
}
