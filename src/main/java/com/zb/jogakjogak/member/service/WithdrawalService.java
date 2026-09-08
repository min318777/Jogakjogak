package com.zb.jogakjogak.member.service;


import com.zb.jogakjogak.global.exception.AuthException;
import com.zb.jogakjogak.global.exception.MemberErrorCode;
import com.zb.jogakjogak.member.entity.Member;
import com.zb.jogakjogak.member.entity.OAuth2Info;
import com.zb.jogakjogak.member.repository.MemberRepository;
import com.zb.jogakjogak.security.jwt.JWTUtil;
import com.zb.jogakjogak.security.service.BlacklistService;
import com.zb.jogakjogak.security.service.RefreshTokenRedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WithdrawalService {
    private final MemberRepository memberRepository;
    private final RefreshTokenRedisService refreshTokenRedisService;
    private final BlacklistService blacklistService;
    private final JWTUtil jwtUtil;
    private final KakaoWithdrawalService kakaoWithdrawalService;
    private final GoogleWithdrawalService googleWithdrawalService;

    public void withdrawMember(Long userId, String accessToken) {
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new AuthException(MemberErrorCode.NOT_FOUND_MEMBER));

        OAuth2Info oAuth2Info = member.getOauth2Info().stream().findFirst()
                .orElseThrow(() -> new AuthException(MemberErrorCode.NOT_FOUND_OAUTH_PROVIDER));
        String provider = oAuth2Info.getProvider();
        String providerId = oAuth2Info.getProviderId();

        if(provider.equalsIgnoreCase("kakao")){
            kakaoWithdrawalService.unlinkKakaoMember(providerId);
        }else{
            String oauthAccessToken = oAuth2Info.getAccessToken();
            googleWithdrawalService.unlinkGoogleMember(oauthAccessToken);
        }

        refreshTokenRedisService.revokeAll(member.getId());
        blacklistAccessToken(accessToken);
        memberRepository.delete(member);
    }

    private void blacklistAccessToken(String accessToken) {
        if (accessToken == null) {
            return;
        }
        String jti = jwtUtil.getJti(accessToken);
        long remainingMs = jwtUtil.getExpiration(accessToken).getTime() - System.currentTimeMillis();
        blacklistService.addToBlacklist(jti, remainingMs);
    }

    public void withdrawByKakaoCallback(String kakaoId) {
        memberRepository.findByOauth2ProviderAndProviderId("kakao", kakaoId)
                .ifPresent(member -> {
                    refreshTokenRedisService.revokeAll(member.getId());
                    memberRepository.delete(member);
                });
    }
}
