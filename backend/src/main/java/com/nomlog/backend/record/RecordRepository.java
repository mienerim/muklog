package com.nomlog.backend.record;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecordRepository extends JpaRepository<Record, Long> {

    List<Record> findByUserIdAndEatenAtBetween(Long userId, LocalDateTime start, LocalDateTime end);

    @Query("""
        SELECT r.foodName FROM Record r
        WHERE r.user.id = :userId AND LOWER(r.foodName) LIKE LOWER(CONCAT(:prefix, '%'))
        GROUP BY r.foodName
        ORDER BY MAX(r.eatenAt) DESC
        """)
    List<String> findDistinctFoodNamesByUserIdAndPrefix(@Param("userId") Long userId, @Param("prefix") String prefix, Pageable pageable);

    @Query("""
        SELECT r.category FROM Record r
        WHERE r.user.id = :userId AND r.category IS NOT NULL
          AND LOWER(r.category) LIKE LOWER(CONCAT(:prefix, '%'))
        GROUP BY r.category
        ORDER BY MAX(r.eatenAt) DESC
        """)
    List<String> findDistinctCategoriesByUserIdAndPrefix(@Param("userId") Long userId, @Param("prefix") String prefix, Pageable pageable);
}
