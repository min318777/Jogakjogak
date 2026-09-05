package com.zb.jogakjogak.member.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@Entity
@Table(name = "oauth2_info")
public class OAuth2Info {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String provider;

    @Column(nullable = false)
    private String providerId;

    private String accessToken;

    @ManyToOne
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    public void updateAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}

