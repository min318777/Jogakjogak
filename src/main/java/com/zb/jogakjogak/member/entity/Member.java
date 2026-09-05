package com.zb.jogakjogak.member.entity;

import com.zb.jogakjogak.event.entity.Event;
import com.zb.jogakjogak.jobdescription.entity.JD;
import com.zb.jogakjogak.notification.entity.Notification;
import com.zb.jogakjogak.resume.entity.Resume;
import com.zb.jogakjogak.member.config.EmailEncryptor;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Convert(converter = EmailEncryptor.class)
    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    private String name;

    private String nickname;

    @Column(nullable = false)
    private boolean isNotificationEnabled;

    @Column(nullable = false)
    private boolean isOnboarded;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private LocalDateTime createdAt;

    private LocalDateTime lastLoginAt;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OAuth2Info> oauth2Info = new ArrayList<>();

    @OneToOne(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Resume resume;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<JD> jdList = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Notification> notification;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Event> eventList;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.lastLoginAt = LocalDateTime.now();
    }

    public void updateExistingMember(String email) {
        this.email = email;
        this.lastLoginAt = LocalDateTime.now();
    }

    public void updateMember(String nickname, Boolean isNotificationEnabled) {
        if (nickname != null) {
            this.nickname = nickname;
        }

        if (isNotificationEnabled != null) {
            this.isNotificationEnabled = isNotificationEnabled;
        }
    }

    public void assignNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updateNotificationEnabled(boolean isNotificationEnabled) {
        this.isNotificationEnabled = isNotificationEnabled;
    }

    public void updateOnboarded(boolean isOnboarded) {
        this.isOnboarded = isOnboarded;
    }

    public void setResume(Resume resume) {
        this.resume = resume;
        if (resume != null && (resume.getMember() == null || !resume.getMember().equals(this))) {
            resume.setMember(this);
        }
    }
}
