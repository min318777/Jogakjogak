package com.zb.jogakjogak.security.service;


import com.zb.jogakjogak.global.exception.AuthException;
import com.zb.jogakjogak.global.exception.MemberErrorCode;
import com.zb.jogakjogak.security.Role;
import com.zb.jogakjogak.security.Token;
import com.zb.jogakjogak.security.dto.ReissueResultDto;
import com.zb.jogakjogak.security.entity.Member;
import com.zb.jogakjogak.security.jwt.JWTUtil;
import com.zb.jogakjogak.security.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReissueService {

    private final JWTUtil jwtUtil;
    private final RefreshTokenRedisService refreshTokenRedisService;
    private final MemberRepository memberRepository;
    private static final long ACCESS_TOKEN_EXPIRATION = 1000 * 60 * 30L;
    private static final long REFRESH_TOKEN_EXPIRATION = 7 * 24 * 60 * 60 * 1000L;

    public ReissueResultDto reissue(String refreshToken) {

        jwtUtil.validateToken(refreshToken, Token.REFRESH_TOKEN);

        Long userId = Long.parseLong(jwtUtil.getUserId(refreshToken));

        String storedToken = refreshTokenRedisService.get(userId)
                .orElseThrow(() -> new AuthException(MemberErrorCode.NOT_FOUND_TOKEN));

        if (!storedToken.equals(refreshToken)) {
            refreshTokenRedisService.delete(userId);
            throw new AuthException(MemberErrorCode.TOKEN_THEFT_DETECTED);
        }

        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new AuthException(MemberErrorCode.NOT_FOUND_MEMBER));

        String provider = member.getOauth2Info().get(0).getProvider();
        String username = member.getUsername();

        String newAccess = jwtUtil.createAccessToken(userId, provider, username, Role.USER.toString(), ACCESS_TOKEN_EXPIRATION, Token.ACCESS_TOKEN);
        String newRefresh = jwtUtil.createRefreshToken(userId, REFRESH_TOKEN_EXPIRATION, Token.REFRESH_TOKEN);

        refreshTokenRedisService.save(userId, newRefresh);

        return ReissueResultDto.builder()
                .newAccessToken(newAccess)
                .newRefreshToken(newRefresh)
                .build();
    }
}
