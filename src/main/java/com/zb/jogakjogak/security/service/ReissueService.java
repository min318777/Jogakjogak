package com.zb.jogakjogak.security.service;


import com.zb.jogakjogak.global.exception.AuthException;
import com.zb.jogakjogak.global.exception.MemberErrorCode;
import com.zb.jogakjogak.security.Role;
import com.zb.jogakjogak.security.Token;
import com.zb.jogakjogak.security.dto.ReissueResultDto;
import com.zb.jogakjogak.security.entity.Member;
import com.zb.jogakjogak.security.jwt.JWTUtil;
import com.zb.jogakjogak.security.repository.MemberRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReissueService {

    private final JWTUtil jwtUtil;
    private final RefreshTokenRedisService refreshTokenRedisService;
    private final MemberRepository memberRepository;

    public ReissueResultDto reissue(String refreshToken) {

        Claims claims = jwtUtil.validateToken(refreshToken, Token.REFRESH_TOKEN);

        Long userId = Long.parseLong(jwtUtil.getUserId(claims));
        String jti = jwtUtil.getJti(claims);

        if (!refreshTokenRedisService.exists(userId, jti)) {
            refreshTokenRedisService.revokeAll(userId);
            throw new AuthException(MemberErrorCode.TOKEN_THEFT_DETECTED);
        }

        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new AuthException(MemberErrorCode.NOT_FOUND_MEMBER));

        String provider = member.getOauth2Info().get(0).getProvider();
        String username = member.getUsername();

        String newAccess = jwtUtil.createAccessToken(userId, provider, username, Role.USER.toString(), Token.ACCESS_TOKEN);
        String newRefresh = jwtUtil.createRefreshToken(userId, Token.REFRESH_TOKEN);

        refreshTokenRedisService.revoke(userId, jti);
        refreshTokenRedisService.save(userId, jwtUtil.getJti(newRefresh));

        return ReissueResultDto.builder()
                .newAccessToken(newAccess)
                .newRefreshToken(newRefresh)
                .build();
    }
}
