package com.nomlog.backend.record;

import com.nomlog.backend.common.BaseTimeEntity;
import com.nomlog.backend.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "records")
public class Record extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String foodName;

    // Optional: preset or free-text category (PRD 5.1).
    private String category;

    private String imageUrl;

    @Column(nullable = false)
    private LocalDateTime eatenAt;

    protected Record() {
    }

    public Record(User user, String foodName, String category, String imageUrl, LocalDateTime eatenAt) {
        this.user = user;
        this.foodName = foodName;
        this.category = category;
        this.imageUrl = imageUrl;
        this.eatenAt = eatenAt;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getFoodName() {
        return foodName;
    }

    public String getCategory() {
        return category;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public LocalDateTime getEatenAt() {
        return eatenAt;
    }

    public void update(String foodName, String category, String imageUrl, LocalDateTime eatenAt) {
        if (foodName != null) {
            this.foodName = foodName;
        }
        if (category != null) {
            this.category = category;
        }
        if (imageUrl != null) {
            this.imageUrl = imageUrl;
        }
        if (eatenAt != null) {
            this.eatenAt = eatenAt;
        }
    }
}
