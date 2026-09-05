package com.zb.jogakjogak.jobDescription.repository;

import com.zb.jogakjogak.jobDescription.entity.JD;
import com.zb.jogakjogak.security.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface JDRepository extends JpaRepository<JD, Long>, JDRepositoryCustom {

    List<JD> findAllByMember(Member member);

    void deleteAllByMemberAndIsCreatedWithResumeFalse(Member member);

    @Modifying
    @Query("UPDATE JD j SET j.notificationCount = j.notificationCount + 1, j.lastNotifiedAt = :lastNotifiedAt WHERE j.id IN :ids")
    void updateNotificationFields(@Param("ids") List<Long> ids, @Param("lastNotifiedAt") LocalDateTime lastNotifiedAt);
}
