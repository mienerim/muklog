package com.nomlog.backend.record;

import com.nomlog.backend.user.User;
import com.nomlog.backend.user.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecordService {

    private static final int AUTOCOMPLETE_LIMIT = 10;

    private final RecordRepository recordRepository;
    private final UserRepository userRepository;

    public RecordService(RecordRepository recordRepository, UserRepository userRepository) {
        this.recordRepository = recordRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public RecordResponse create(Long userId, RecordCreateRequest request) {
        User userRef = userRepository.getReferenceById(userId);
        LocalDateTime eatenAt = request.eatenAt() != null ? request.eatenAt() : LocalDateTime.now();

        Record record = new Record(userRef, request.foodName(), request.category(), request.imageUrl(), eatenAt);
        recordRepository.save(record);

        return RecordResponse.from(record);
    }

    public List<RecordResponse> listByDate(Long userId, LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        return recordRepository.findByUserIdAndEatenAtBetween(userId, start, end).stream()
            .map(RecordResponse::from)
            .toList();
    }

    @Transactional
    public RecordResponse update(Long userId, Long recordId, RecordUpdateRequest request) {
        Record record = findOwnedOrThrow(userId, recordId);
        record.update(request.foodName(), request.category(), request.imageUrl(), request.eatenAt());
        return RecordResponse.from(record);
    }

    @Transactional
    public void delete(Long userId, Long recordId) {
        Record record = findOwnedOrThrow(userId, recordId);
        recordRepository.delete(record);
    }

    public List<String> autocomplete(Long userId, AutocompleteField field, String query) {
        PageRequest limit = PageRequest.of(0, AUTOCOMPLETE_LIMIT);

        return switch (field) {
            case FOOD_NAME -> recordRepository.findDistinctFoodNamesByUserIdAndPrefix(userId, query, limit);
            case CATEGORY -> recordRepository.findDistinctCategoriesByUserIdAndPrefix(userId, query, limit);
        };
    }

    private Record findOwnedOrThrow(Long userId, Long recordId) {
        return recordRepository.findById(recordId)
            .filter(record -> record.getUser().getId().equals(userId))
            .orElseThrow(() -> new RecordNotFoundException(recordId));
    }
}
