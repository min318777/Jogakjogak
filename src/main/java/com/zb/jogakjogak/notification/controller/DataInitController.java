package com.zb.jogakjogak.notification.controller;

import com.zb.jogakjogak.notification.dto.ResponseDto;
import com.zb.jogakjogak.security.Role;
import com.zb.jogakjogak.security.entity.Member;
import com.zb.jogakjogak.security.repository.MemberRepository;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Hidden
@Profile("perf-test")
@RestController
@RequestMapping("/biz/init")
public class DataInitController {

    private static final int TOTAL_MEMBERS = 1_000;
    private static final int ELIGIBLE_JD_PER_MEMBER = 100;   // 알림 대상: 10만건
    private static final int INELIGIBLE_JD_PER_MEMBER = 0;   // 알림 제외: 없음
    private static final int JDBC_BATCH_SIZE = 5_000;
    private static final String TEST_USERNAME_PREFIX = "perf_test_";
    private static final String SMALL_TEST_USERNAME_PREFIX = "small_test_";

    private final MemberRepository memberRepository;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public DataInitController(
            MemberRepository memberRepository,
            @Qualifier("mainDBSource") DataSource mainDBSource) {
        this.memberRepository = memberRepository;
        this.jdbcTemplate = new JdbcTemplate(mainDBSource);
    }

    @PostMapping("/members")
    public ResponseDto initMembers() {
        long existing = memberRepository.findAll().stream()
                .filter(m -> m.getUsername() != null && m.getUsername().startsWith(TEST_USERNAME_PREFIX))
                .count();
        if (existing > 0) {
            return new ResponseDto("이미 테스트 Member가 존재합니다. /biz/init/clean 후 재시도하세요.");
        }

        List<Member> members = new ArrayList<>(TOTAL_MEMBERS);
        for (int i = 1; i <= TOTAL_MEMBERS; i++) {
            members.add(Member.builder()
                    .username(TEST_USERNAME_PREFIX + i)
                    .email("perftest" + i + "@test.com")
                    .nickname("테스터" + i)
                    .role(Role.USER)
                    .isNotificationEnabled(true)
                    .lastLoginAt(LocalDateTime.now())
                    .build());
        }
        memberRepository.saveAll(members);
        log.info("테스트 Member {}명 생성 완료", TOTAL_MEMBERS);
        return new ResponseDto("Member " + TOTAL_MEMBERS + "명 생성 완료");
    }

    @PostMapping("/jds")
    public ResponseDto initJds() {
        List<Long> memberIds = memberRepository.findAll().stream()
                .filter(m -> m.getUsername() != null && m.getUsername().startsWith(TEST_USERNAME_PREFIX))
                .map(Member::getId)
                .toList();

        if (memberIds.isEmpty()) {
            return new ResponseDto("테스트 Member가 없습니다. /biz/init/members 먼저 실행하세요.");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fourDaysAgo = now.minusDays(4);
        LocalDateTime oneYearLater = now.plusYears(1);

        // 알림 대상: updated_at=4일전, is_alarm_on=true, notification_count=0
        int total = ELIGIBLE_JD_PER_MEMBER * memberIds.size();
        log.info("알림 대상 JD {}건 삽입 시작...", total);
        bulkInsertJds(memberIds, ELIGIBLE_JD_PER_MEMBER, true, fourDaysAgo, oneYearLater, now);

        log.info("JD 총 {}건 삽입 완료", total);
        return new ResponseDto("JD " + total + "건 삽입 완료");
    }

    @PostMapping("/small")
    public ResponseDto initSmall() {
        long existing = memberRepository.findAll().stream()
                .filter(m -> m.getUsername() != null && m.getUsername().startsWith(SMALL_TEST_USERNAME_PREFIX))
                .count();
        if (existing > 0) {
            return new ResponseDto("이미 small 테스트 데이터가 존재합니다. /biz/init/clean 후 재시도하세요.");
        }

        int smallMembers = 3;
        int eligiblePerMember = 70; // 3 * 70 = 210건 (청크 100 기준 3페이지)
        List<Member> members = new ArrayList<>(smallMembers);
        for (int i = 1; i <= smallMembers; i++) {
            members.add(Member.builder()
                    .username(SMALL_TEST_USERNAME_PREFIX + i)
                    .email("small" + i + "@test.com")
                    .nickname("스몰테스터" + i)
                    .role(Role.USER)
                    .isNotificationEnabled(true)
                    .lastLoginAt(LocalDateTime.now())
                    .build());
        }
        memberRepository.saveAll(members);

        List<Long> memberIds = memberRepository.findAll().stream()
                .filter(m -> m.getUsername() != null && m.getUsername().startsWith(SMALL_TEST_USERNAME_PREFIX))
                .map(Member::getId)
                .toList();

        LocalDateTime now = LocalDateTime.now();
        bulkInsertJds(memberIds, eligiblePerMember, true, now.minusDays(4), now.plusYears(1), now);

        int total = eligiblePerMember * memberIds.size();
        log.info("small 테스트 데이터 생성 완료: member {}명, JD {}건", smallMembers, total);
        return new ResponseDto("small 테스트 데이터 생성 완료 (member: " + smallMembers + "명, eligible JD: " + total + "건)");
    }

    @DeleteMapping("/clean")
    public ResponseDto clean() {
        List<Long> memberIds = memberRepository.findAll().stream()
                .filter(m -> m.getUsername() != null &&
                        (m.getUsername().startsWith(TEST_USERNAME_PREFIX) || m.getUsername().startsWith(SMALL_TEST_USERNAME_PREFIX)))
                .map(Member::getId)
                .toList();

        if (!memberIds.isEmpty()) {
            String idList = String.join(",", memberIds.stream().map(String::valueOf).toList());
            jdbcTemplate.update("DELETE FROM notification WHERE member_id IN (" + idList + ")");
            jdbcTemplate.update("DELETE FROM job_description WHERE member_id IN (" + idList + ")");
            jdbcTemplate.update("DELETE FROM member WHERE username LIKE '" + TEST_USERNAME_PREFIX + "%'" +
                    " OR username LIKE '" + SMALL_TEST_USERNAME_PREFIX + "%'");
        }

        log.info("테스트 데이터 삭제 완료. member: {}명", memberIds.size());
        return new ResponseDto("테스트 데이터 삭제 완료");
    }

    private void bulkInsertJds(List<Long> memberIds, int countPerMember,
                                boolean alarmOn, LocalDateTime updatedAt,
                                LocalDateTime endedAt, LocalDateTime now) {
        String sql = "INSERT INTO job_description " +
                "(title, is_bookmark, company_name, job, content, jd_url, memo, is_alarm_on, notification_count, " +
                "is_created_with_resume, ended_at, member_id, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        List<Object[]> batch = new ArrayList<>(JDBC_BATCH_SIZE);
        Timestamp updatedAtTs = Timestamp.valueOf(updatedAt);
        Timestamp endedAtTs = Timestamp.valueOf(endedAt);
        Timestamp createdAtTs = Timestamp.valueOf(now);

        for (Long memberId : memberIds) {
            for (int i = 0; i < countPerMember; i++) {
                batch.add(new Object[]{
                        "테스트JD", false, "테스트기업", "테스트직무", "테스트내용", "https://test.com", "",
                        alarmOn, 0, false,
                        endedAtTs, memberId, createdAtTs, updatedAtTs
                });

                if (batch.size() == JDBC_BATCH_SIZE) {
                    jdbcTemplate.batchUpdate(sql, batch);
                    batch.clear();
                }
            }
        }
        if (!batch.isEmpty()) {
            jdbcTemplate.batchUpdate(sql, batch);
        }
    }
}