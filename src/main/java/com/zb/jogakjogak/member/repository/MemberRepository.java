package com.zb.jogakjogak.member.repository;

import com.zb.jogakjogak.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByUsername(String username);

    @Query("SELECT m FROM Member m LEFT JOIN FETCH m.oauth2Info WHERE m.username = :username")
    Optional<Member> findByUsernameWithOauth2Info(@Param("username") String username);

    @Query("SELECT m FROM Member m JOIN m.oauth2Info o WHERE o.provider = :provider AND o.providerId = :providerId")
    Optional<Member> findByOauth2ProviderAndProviderId(@Param("provider") String provider, @Param("providerId") String providerId);

    boolean existsByNickname(String nickname);

    List<Member> findByNicknameIsNull();

}
