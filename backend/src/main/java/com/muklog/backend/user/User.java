package com.muklog.backend.user;

import com.muklog.backend.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Google OAuth2 "sub" claim — stable unique identifier for the logged-in account.
    @Column(nullable = false, unique = true)
    private String googleSub;

    @Column(nullable = false)
    private String email;

    private String nickname;

    protected User() {
    }

    public User(String googleSub, String email, String nickname) {
        this.googleSub = googleSub;
        this.email = email;
        this.nickname = nickname;
    }

    public Long getId() {
        return id;
    }

    public String getGoogleSub() {
        return googleSub;
    }

    public String getEmail() {
        return email;
    }

    public String getNickname() {
        return nickname;
    }
}
