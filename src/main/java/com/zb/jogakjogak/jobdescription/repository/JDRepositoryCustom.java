package com.zb.jogakjogak.jobdescription.repository;

import com.zb.jogakjogak.jobdescription.entity.JD;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Optional;

public interface JDRepositoryCustom {

    /**
     * JD ID만으로 JD, Member, ToDoList를 즉시 로딩하여 조회합니다.
     * 존재 여부(404)와 소유권(403)을 구분해서 검증할 때 사용합니다.
     */
    Optional<JD> findJdWithMemberAndToDoListsById(Long jdId);

    /**
     * 특정 Member의 모든 JD 목록을 페이징하여 조회합니다.
     * 각 JD와 연관된 ToDoList를 즉시 로딩하여 N+1 문제를 방지합니다.
     *
     */
    Page<JD> findAllJdsByMemberIdWithToDoLists(Long memberId, Pageable pageable, String showOnly);

    /**
     * 특정 날짜 이전에 업데이트되지 않았고, 마감일이 현재 시간 이후이며, 알림이 켜져 있는 JD 목록을 페이징하여 조회합니다.
     *
     * @param oldDate  업데이트 기준 날짜
     * @param pageable 페이징 및 정렬 정보
     * @return 조건에 맞는 JD 목록
     */
    Page<JD> findNotUpdatedJdByQueryDsl(LocalDateTime oldDate, LocalDateTime todayStart, Pageable pageable);

    Page<JD> findTodayNotifiedJds(LocalDateTime todayStart, Pageable pageable);

    Long findAllJdCountByMemberId(Long memberId);

    /**
     * showOnly 필터가 적용된 회원의 전체 JD 집합에 대한 통계(지원완료 수, 완료/전체 조각 수, 퍼펙트 JD 수)를 집계합니다.
     */
    JdStatsDto getJdStats(Long memberId, String showOnly);
}
