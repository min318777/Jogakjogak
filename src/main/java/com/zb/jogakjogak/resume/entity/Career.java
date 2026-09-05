package com.zb.jogakjogak.resume.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Career {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private LocalDate joinedAt;
    private LocalDate quitAt;
    @Column(nullable = false)
    private Boolean isWorking;
    @Column(nullable = false, length = 100)
    private String companyName;
    @Column(nullable = false, length = 2000)
    private String workPerformance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    public static Career of(LocalDate joinedAt, LocalDate quitAt, Boolean isWorking,
                             String companyName, String workPerformance, Resume resume) {
        return Career.builder()
                .joinedAt(joinedAt)
                .quitAt(quitAt)
                .isWorking(isWorking)
                .companyName(companyName)
                .workPerformance(workPerformance)
                .resume(resume)
                .build();
    }

}
