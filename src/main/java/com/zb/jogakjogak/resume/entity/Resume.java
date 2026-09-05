package com.zb.jogakjogak.resume.entity;

import com.zb.jogakjogak.global.BaseEntity;
import com.zb.jogakjogak.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Set;

@Entity
@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Resume extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 30)
    private String title;
    @Column(nullable = false, length = 5000)
    private String content;
    @Builder.Default
    @Column(nullable = false)
    private boolean isNewcomer = true;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @OneToMany(mappedBy = "resume", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Career> careerList;
    @OneToMany(mappedBy = "resume", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Education> educationList;
    @OneToMany(mappedBy = "resume", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Skill> skillList;

    public void modify(String title, String content) {
        if (title != null) {
            this.title = title;
        }
        if (content != null) {
            this.content = content;
        }
    }

    public void update(String content, Boolean isNewcomer) {
        if (content != null) {
            this.content = content;
        }
        if (isNewcomer != null) {
            this.isNewcomer = isNewcomer;
        }
    }

    public void setMember(Member member) {
        this.member = member;
    }
}
