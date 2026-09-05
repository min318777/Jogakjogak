package com.zb.jogakjogak.resume.entity;

import com.zb.jogakjogak.resume.type.EducationLevel;
import com.zb.jogakjogak.resume.type.EducationStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Education {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EducationLevel level;
    @Column(nullable = false, length = 225)
    private String majorField;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EducationStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    public static Education of(EducationLevel level, String majorField, EducationStatus status, Resume resume) {
        return Education.builder()
                .level(level)
                .majorField(majorField)
                .status(status)
                .resume(resume)
                .build();
    }
}
