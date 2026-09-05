package com.zb.jogakjogak.jobdescription.entity;

import com.zb.jogakjogak.global.BaseEntity;
import com.zb.jogakjogak.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "job_description")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JD extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String title;

    @Column(nullable = false)
    private boolean isBookmark;

    @Column(nullable = false, length = 100)
    private String companyName;

    @Column(nullable = false)
    private String job;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT", length = 10000)
    private String content;

    @Column(nullable = false)
    private String jdUrl;

    @Column
    private String memo;

    @Column(nullable = false)
    private boolean isAlarmOn;

    @Column
    private LocalDateTime applyAt;

    @Builder.Default
    @Column(nullable = false)
    private int notificationCount = 0;

    @Column(nullable = false)
    private boolean isCreatedWithResume;

    @Column
    private LocalDateTime lastNotifiedAt;

    @Column(columnDefinition = "DATE")
    private LocalDateTime endedAt;

    @Builder.Default
    @OneToMany(mappedBy = "jd", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ToDoList> toDoLists = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    public void addToDoList(ToDoList toDoList) {
        if (this.toDoLists == null) {
            this.toDoLists = new ArrayList<>();
        }
        this.toDoLists.add(toDoList);
        toDoList.setJd(this);
    }

    public void updateAlarmStatus(boolean isAlarmOn) {
        this.isAlarmOn = isAlarmOn;
    }

    public void updateBookmarkStatus(boolean isBookmark) {
        this.isBookmark = isBookmark;
    }

    public void markJdAsApplied() {
        this.applyAt = LocalDateTime.now();
    }

    public void unMarkJdAsApplied() {
        this.applyAt = null;
    }

    public void updateMemo(String memo) {
        this.memo = memo;
    }

    public void updateJd(String title, String companyName, String job, String jdUrl, LocalDateTime endedAt) {
        if (title != null) {
            this.title = title;
        }
        if (companyName != null) {
            this.companyName = companyName;
        }
        if (job != null) {
            this.job = job;
        }
        if (jdUrl != null) {
            this.jdUrl = jdUrl;
        }
        if (endedAt != null) {
            this.endedAt = endedAt;
        }
    }

    public void markAsUpdated() {
        this.notificationCount = 0;
        this.lastNotifiedAt = null;
    }
}
