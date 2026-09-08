package com.zb.jogakjogak.jobdescription.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import com.zb.jogakjogak.event.service.EventService;
import com.zb.jogakjogak.global.exception.JDErrorCode;
import com.zb.jogakjogak.global.exception.JDException;
import com.zb.jogakjogak.jobdescription.dto.request.*;
import com.zb.jogakjogak.jobdescription.dto.response.*;
import com.zb.jogakjogak.jobdescription.entity.JD;
import com.zb.jogakjogak.jobdescription.entity.ToDoList;
import com.zb.jogakjogak.jobdescription.repository.JDRepository;
import com.zb.jogakjogak.jobdescription.repository.JdStatsDto;
import com.zb.jogakjogak.jobdescription.type.ToDoListType;
import com.zb.jogakjogak.resume.entity.Resume;
import com.zb.jogakjogak.member.entity.Role;
import com.zb.jogakjogak.member.entity.Member;
import com.zb.jogakjogak.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JDServiceTest {

    @Mock
    private LLMService llmService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private JDService jdService;

    @Mock
    private JDRepository jdRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private EventService eventService;

    private JDCreateRequestDto jdRequestDto;
    private String mockLLMAnalysisJsonString;
    private List<ToDoListDto> mockToDoListDtosForLLM;
    private Faker faker;
    private Member mockMember;
    private Pageable pageable;
    private JD testJd;
    private Resume mockResume;

    @BeforeEach
    void setUp() {
        faker = new Faker();

        mockResume = Resume.builder()
                .id(1L)
                .title("테스트 이력서")
                .content("테스트 내용")
                .member(mockMember)
                .build();

        mockMember = Member.builder()
                .id(1L)
                .username("testUser")
                .email("test@example.com")
                .password("password123")
                .role(Role.USER)
                .resume(mockResume)
                .isOnboarded(false)
                .build();

        jdRequestDto = JDCreateRequestDto.builder()
                .title("시니어 백엔드 개발자 채용")
                .jdUrl("https://example.com/jd/123")
                .companyName(faker.company().name())
                .job(faker.job().title())
                .content(faker.lorem().paragraph())
                .endedAt(faker.date().future(365, TimeUnit.DAYS).toInstant().atZone(ZoneId.systemDefault()).toLocalDate().atStartOfDay())
                .build();

        mockLLMAnalysisJsonString = "[" +
                "  {" +
                "    \"type\": \"STRUCTURAL_COMPLEMENT_PLAN\"," +
                "    \"title\": \"이력서 Java/Spring Boot 경험 강조\"," +
                "    \"description\": \"이력서에 Spring Boot 프로젝트 경험을 구체적으로 서술합니다.\"," +
                "    \"memo\": \"\"," +
                "    \"isDone\": false" +
                "  }," +
                "  {" +
                "    \"type\": \"CONTENT_EMPHASIS_REORGANIZATION_PROPOSAL\"," +
                "    \"title\": \"AWS 클라우드 경험 구체화\"," +
                "    \"description\": \"AWS EC2 배포 경험을 수치와 함께 명확히 기술합니다.\"," +
                "    \"memo\": \"\"," +
                "    \"isDone\": false" +
                "  }" +
                "]";

        ToDoListDto llmDto1 = new ToDoListDto(ToDoListType.STRUCTURAL_COMPLEMENT_PLAN, "이력서 Java/Spring Boot 경험 강조", "이력서에 Spring Boot 프로젝트 경험을 구체적으로 서술합니다.", "", false);
        ToDoListDto llmDto2 = new ToDoListDto(ToDoListType.CONTENT_EMPHASIS_REORGANIZATION_PROPOSAL, "AWS 클라우드 경험 구체화", "AWS EC2 배포 경험을 수치와 함께 명확히 기술합니다.", "", false);
        mockToDoListDtosForLLM = Arrays.asList(llmDto1, llmDto2);

        pageable = PageRequest.of(0, 11, Sort.by("createdAt").descending());

        testJd = JD.builder()
                .id(1L)
                .title("Test Job")
                .isBookmark(false)
                .member(mockMember)
                .applyAt(null)
                .build();
    }

    @Test
    @DisplayName("LLM 분석 서비스 성공 테스트 - JD 및 ToDoList 저장 포함 (Gemini)")
    void llmAnalyze_success() throws JsonProcessingException {
        // given
        when(llmService.generateTodoListJson(anyString(), anyString(), anyString()))
                .thenReturn(mockLLMAnalysisJsonString);
        when(objectMapper.readValue(eq(mockLLMAnalysisJsonString), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(mockToDoListDtosForLLM);
        when(jdRepository.save(any(JD.class))).thenAnswer(invocation -> {
            JD originalJd = invocation.getArgument(0);
            return JD.builder()
                    .id(1L)
                    .title(originalJd.getTitle())
                    .isBookmark(originalJd.isBookmark())
                    .companyName(originalJd.getCompanyName())
                    .job(originalJd.getJob())
                    .content(originalJd.getContent())
                    .jdUrl(originalJd.getJdUrl())
                    .endedAt(originalJd.getEndedAt())
                    .memo(originalJd.getMemo())
                    .member(originalJd.getMember())
                    .isAlarmOn(originalJd.isAlarmOn())
                    .applyAt(originalJd.getApplyAt())
                    .toDoLists(originalJd.getToDoLists())
                    .build();
        });
        when(memberRepository.findById(mockMember.getId())).thenReturn(Optional.of(mockMember));

        // when
        JDResponseDto result = jdService.llmAnalyze(jdRequestDto, mockMember.getId());

        // then
        assertNotNull(result);
        assertEquals(jdRequestDto.getTitle(), result.getTitle());
        assertEquals(jdRequestDto.getJdUrl(), result.getJdUrl());
        assertEquals(jdRequestDto.getEndedAt(), result.getEndedAt());
        assertNotNull(result.getToDoLists());
        assertFalse(result.getToDoLists().isEmpty());
        assertEquals(mockToDoListDtosForLLM.size(), result.getToDoLists().size());
        assertEquals(mockToDoListDtosForLLM.get(0).getTitle(), result.getToDoLists().get(0).getTitle());
        assertEquals(mockToDoListDtosForLLM.get(0).getContent(), result.getToDoLists().get(0).getContent());
        assertEquals(mockToDoListDtosForLLM.get(0).getCategory(), result.getToDoLists().get(0).getCategory());
        assertEquals(mockToDoListDtosForLLM.get(0).getMemo(), result.getToDoLists().get(0).getMemo());
        assertEquals(mockToDoListDtosForLLM.get(0).isDone(), result.getToDoLists().get(0).isDone());

        // verify
        verify(llmService, times(1)).generateTodoListJson(anyString(), anyString(), anyString());
        verify(objectMapper, times(1)).readValue(eq(mockLLMAnalysisJsonString), any(com.fasterxml.jackson.core.type.TypeReference.class));
        verify(jdRepository, times(1)).save(any(JD.class));

        ArgumentCaptor<JD> jdCaptor = ArgumentCaptor.forClass(JD.class);
        verify(jdRepository).save(jdCaptor.capture());
        JD savedJdSentToRepo = jdCaptor.getValue();
        assertNotNull(savedJdSentToRepo.getToDoLists());
        assertEquals(mockToDoListDtosForLLM.size(), savedJdSentToRepo.getToDoLists().size());
        assertEquals(mockToDoListDtosForLLM.get(0).getTitle(), savedJdSentToRepo.getToDoLists().get(0).getTitle());
    }

    @Test
    @DisplayName("LLM 분석 서비스 실패 테스트 - JD 제한 20개 초과 시 JDException 발생")
    void llmAnalyze_failure_jdLimitExceeded() {
        // given
        Resume mockResume = Resume.builder()
                .content("테스트이력서")
                .build();
        mockMember.setResume(mockResume);

        when(memberRepository.findById(mockMember.getId())).thenReturn(Optional.of(mockMember));
        when(jdRepository.findAllJdCountByMemberId(mockMember.getId())).thenReturn(20L);

        // when & then
        JDException thrown = assertThrows(JDException.class, () ->
                jdService.llmAnalyze(jdRequestDto, mockMember.getId()));

        assertEquals(JDErrorCode.JD_LIMIT_EXCEEDED, thrown.getErrorCode());

        // verify
        verify(jdRepository, times(2)).findAllJdCountByMemberId(mockMember.getId());
        verify(llmService, never()).generateTodoListJson(anyString(), anyString(), anyString());
        verify(jdRepository, never()).save(any(JD.class));
    }

    @Test
    @DisplayName("LLM 분석 서비스 JsonProcessingException 발생 시 JDException 던지는지 테스트 (Gemini)")
    void llmAnalyze_failure_jsonProcessingException() throws JsonProcessingException {
        // given
        when(llmService.generateTodoListJson(anyString(), anyString(), anyString()))
                .thenReturn("invalid json string from LLM");

        when(objectMapper.readValue(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenThrow(mock(JsonProcessingException.class));
        when(memberRepository.findById(mockMember.getId())).thenReturn(Optional.of(mockMember));

        // when & then
        JDException thrown = assertThrows(JDException.class, () -> jdService.llmAnalyze(jdRequestDto, mockMember.getId()));
        assertEquals(JDErrorCode.FAILED_JSON_PROCESS, thrown.getErrorCode());

        // verify
        verify(llmService, times(1)).generateTodoListJson(anyString(), anyString(), anyString());
        verify(objectMapper, times(1)).readValue(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class));
        verify(jdRepository, never()).save(any(JD.class));
    }

    @Test
    @DisplayName("JD 조회 서비스 실패 테스트 - JD를 찾을 수 없음")
    void getJd_notFound() {
        // Given
        Long nonExistentJdId = 999L;
        when(jdRepository.findJdWithMemberAndToDoListsById(nonExistentJdId)).thenReturn(Optional.empty());

        // When & Then
        JDException thrown = assertThrows(JDException.class, () -> jdService.getJd(nonExistentJdId, mockMember.getId()));
        assertEquals(JDErrorCode.NOT_FOUND_JD, thrown.getErrorCode());

        // Verify
        verify(jdRepository, times(1)).findJdWithMemberAndToDoListsById(nonExistentJdId);
    }

    @Test
    @DisplayName("JD 조회 서비스 실패 테스트 - 권한 없음")
    void getJd_unauthorizedAccess() {
        // Given
        Long jdId = 1L;
        JD mockJd = JD.builder()
                .id(jdId)
                .title("테스트 JD")
                .member(mockMember)
                .build();
        Member otherMember = Member.builder().id(999L).username("otherUser").build();

        when(jdRepository.findJdWithMemberAndToDoListsById(jdId)).thenReturn(Optional.of(mockJd));

        // When & Then
        JDException thrown = assertThrows(JDException.class,
                () -> jdService.getJd(jdId, otherMember.getId()));
        assertEquals(JDErrorCode.UNAUTHORIZED_ACCESS, thrown.getErrorCode());

        verify(jdRepository, times(1)).findJdWithMemberAndToDoListsById(jdId);
    }

    @Test
    @DisplayName("JD 삭제 서비스 성공 테스트 - JD 및 연관된 ToDoList 함께 삭제")
    void deleteJd_success() {
        // Given
        Long jdId = 1L;
        JD mockJd = JD.builder()
                .id(jdId)
                .title(faker.book().title())
                .member(mockMember)
                .companyName(faker.artist().name())
                .build();

        when(jdRepository.findJdWithMemberAndToDoListsById(jdId)).thenReturn(Optional.of(mockJd));
        doNothing().when(jdRepository).deleteById(jdId);

        // When
        jdService.deleteJd(jdId, mockMember.getId());

        // Then
        verify(jdRepository, times(1)).deleteById(mockJd.getId());
        verify(jdRepository, times(1)).findJdWithMemberAndToDoListsById(jdId);
    }

    @Test
    @DisplayName("JD 삭제 서비스 실패 테스트 - JD를 찾을 수 없음")
    void deleteJd_notFound() {
        // Given
        Long nonExistentJdId = 999L;
        when(jdRepository.findJdWithMemberAndToDoListsById(nonExistentJdId)).thenReturn(Optional.empty());

        // When & Then
        JDException thrown = assertThrows(JDException.class, () -> jdService.deleteJd(nonExistentJdId, mockMember.getId()));
        assertEquals(JDErrorCode.NOT_FOUND_JD, thrown.getErrorCode());

        // Verify
        verify(jdRepository, times(1)).findJdWithMemberAndToDoListsById(nonExistentJdId);
        verify(jdRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("JD 삭제 서비스 실패 테스트 - 권한 없음")
    void deleteJd_unauthorizedAccess() {
        // Given
        Long jdId = 1L;
        JD mockJd = JD.builder()
                .id(jdId)
                .title(faker.book().title())
                .member(mockMember)
                .build();
        Member otherMember = Member.builder().id(999L).username("otherUser").build();

        when(jdRepository.findJdWithMemberAndToDoListsById(jdId)).thenReturn(Optional.of(mockJd));

        // When & Then
        JDException thrown = assertThrows(JDException.class,
                () -> jdService.deleteJd(jdId, otherMember.getId()));
        assertEquals(JDErrorCode.UNAUTHORIZED_ACCESS, thrown.getErrorCode());

        verify(jdRepository, times(1)).findJdWithMemberAndToDoListsById(jdId);
        verify(jdRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("JD 알람 상태 변경 서비스 성공 테스트")
    void alarm_success() {
        // Given
        Long jdId = 1L;
        boolean initialAlarmStatus = false;
        boolean newAlarmStatus = true;

        JD mockJd = JD.builder()
                .id(jdId)
                .title("알람 테스트 JD")
                .member(mockMember)
                .isAlarmOn(initialAlarmStatus)
                .build();

        JD mockUpdatedJD = JD.builder()
                .id(jdId)
                .title("알람 테스트 JD")
                .member(mockMember)
                .isAlarmOn(newAlarmStatus)
                .build();

        when(jdRepository.findJdWithMemberAndToDoListsById(jdId)).thenReturn(Optional.of(mockJd));
        when(memberRepository.findById(mockMember.getId())).thenReturn(Optional.of(mockMember));

        // When
        JDAlarmResponseDto result = jdService.alarm(jdId, newAlarmStatus, mockMember.getId());

        // Then
        assertNotNull(result);
        assertEquals(jdId, result.getJdId());
        assertEquals(newAlarmStatus, result.isAlarmOn());


        verify(jdRepository, times(1)).findJdWithMemberAndToDoListsById(jdId);
        assertEquals(newAlarmStatus, mockJd.isAlarmOn());
    }

    @Test
    @DisplayName("JD 알람 상태 변경 서비스 실패 테스트 - JD를 찾을 수 없음")
    void alarm_notFound() {
        // Given
        Long nonExistentJdId = 999L;

        when(jdRepository.findJdWithMemberAndToDoListsById(nonExistentJdId)).thenReturn(Optional.empty());

        // When & Then
        JDException thrown = assertThrows(JDException.class, () -> jdService.alarm(nonExistentJdId, true, mockMember.getId()));
        assertEquals(JDErrorCode.NOT_FOUND_JD, thrown.getErrorCode());

    }

    @Test
    @DisplayName("JD 알람 상태 변경 서비스 실패 테스트 - 권한 없음")
    void alarm_unauthorizedAccess() {
        // Given
        Long jdId = 1L;
        JD mockJd = JD.builder()
                .id(jdId)
                .title("알람 테스트 JD")
                .member(mockMember)
                .isAlarmOn(false)
                .build();
        Member otherMember = Member.builder().id(999L).username("otherUser").build();

        when(jdRepository.findJdWithMemberAndToDoListsById(jdId)).thenReturn(Optional.of(mockJd));

        // When & Then
        JDException thrown = assertThrows(JDException.class,
                () -> jdService.alarm(jdId, true, otherMember.getId()));
        assertEquals(JDErrorCode.UNAUTHORIZED_ACCESS, thrown.getErrorCode());

        verify(jdRepository, times(1)).findJdWithMemberAndToDoListsById(jdId);
        verify(memberRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("JD 목록 성공적으로 조회 및 ToDoList 개수 계산")
    void getAllJds_Success() {
        // Given
        mockMember.setResume(mockResume);

        ToDoList todo1 = ToDoList.builder()
                .id(1L).category(ToDoListType.STRUCTURAL_COMPLEMENT_PLAN).title("투두1").content("내용1").isDone(true).build();
        ToDoList todo2 = ToDoList.builder()
                .id(2L).category(ToDoListType.CONTENT_EMPHASIS_REORGANIZATION_PROPOSAL).title("투두2").content("내용2").isDone(false).build();
        ToDoList todo3 = ToDoList.builder()
                .id(3L).category(ToDoListType.STRUCTURAL_COMPLEMENT_PLAN).title("투두3").content("내용3").isDone(true).build();

        JD jd1 = JD.builder()
                .id(101L).title("백엔드 개발자").companyName("SKC").endedAt(LocalDate.of(2025, 5, 24).atStartOfDay())
                .member(mockMember)
                .build();
        jd1.addToDoList(todo1);
        jd1.addToDoList(todo2);

        JD jd2 = JD.builder()
                .id(102L).title("UX 디렉터").companyName("메리츠화재").endedAt(LocalDate.of(2025, 1, 20).atStartOfDay())
                .member(mockMember)
                .build();
        jd2.addToDoList(todo3);


        List<JD> jds = Arrays.asList(jd1, jd2);
        Page<JD> jdPage = new PageImpl<>(jds, pageable, jds.size());

        when(jdRepository.findAllJdsByMemberIdWithToDoLists(mockMember.getId(), pageable, "normal")).thenReturn(jdPage);
        when(jdRepository.getJdStats(mockMember.getId(), "normal"))
                .thenReturn(new JdStatsDto(2, 0, 2, 3, 1));
        when(memberRepository.findById(mockMember.getId())).thenReturn(Optional.of(mockMember));

        // When
        PagedJdResponseDto resultPage = jdService.getAllJds(mockMember.getId(), pageable, "normal");


        // Then
        verify(jdRepository, times(1)).findAllJdsByMemberIdWithToDoLists(mockMember.getId(), pageable, "normal");

        assertNotNull(resultPage);
        assertNotNull(resultPage.getJds());
        assertEquals(2, resultPage.getJds().size());
        assertEquals(2, resultPage.getTotalElements());
        assertEquals(1, resultPage.getTotalPages());

        assertEquals(2, resultPage.getPostedJdCount());
        assertEquals(0, resultPage.getApplyJdCount());
        assertEquals(2, resultPage.getAllCompletedPieces());
        assertEquals(3, resultPage.getAllTotalPieces());
        assertEquals(1, resultPage.getPerfectJdCount());

        assertNotNull(resultPage.getResume());
        assertEquals(mockResume.getTitle(), resultPage.getResume().getTitle());
        assertEquals(mockResume.getContent(), resultPage.getResume().getContent());


        AllGetJDResponseDto dto1 = resultPage.getJds().get(0);
        assertEquals(101L, dto1.getJd_id());
        assertEquals(2L, dto1.getTotalPieces());
        assertEquals(1L, dto1.getCompletedPieces());

        AllGetJDResponseDto dto2 = resultPage.getJds().get(1);
        assertEquals(102L, dto2.getJd_id());
        assertEquals(1L, dto2.getTotalPieces());
        assertEquals(1L, dto2.getCompletedPieces());

        assertFalse(resultPage.getIsOnboarded());
    }

    @Test
    @DisplayName("회원은 존재하지만 해당 회원의 JD가 없을 때 빈 페이지 반환")
    void getAllJds_NoJdsForMember_ReturnsEmptyPage() {
        // Given
        Page<JD> emptyJdPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(jdRepository.findAllJdsByMemberIdWithToDoLists(mockMember.getId(), pageable, "normal")).thenReturn(emptyJdPage);
        when(jdRepository.getJdStats(mockMember.getId(), "normal"))
                .thenReturn(new JdStatsDto(0, 0, 0, 0, 0));
        when(memberRepository.findById(mockMember.getId())).thenReturn(Optional.of(mockMember));

        // When
        PagedJdResponseDto resultPage = jdService.getAllJds(mockMember.getId(), pageable, "normal");


        // Then
        verify(jdRepository, times(1)).findAllJdsByMemberIdWithToDoLists(mockMember.getId(), pageable, "normal");

        assertNotNull(resultPage);
        assertNotNull(resultPage.getJds());
        assertTrue(resultPage.getJds().isEmpty());
        assertEquals(0, resultPage.getTotalElements());
        assertEquals(0, resultPage.getTotalPages());
        assertNotNull(resultPage.getResume());
        assertEquals(mockResume.getTitle(), resultPage.getResume().getTitle());
    }

    @ParameterizedTest(name = "isBookmark={0}로 변경 요청 시 그대로 반영된다")
    @DisplayName("북마크 상태 업데이트 성공")
    @ValueSource(booleans = {true, false})
    void updateBookmarkStatus_success(boolean isBookmark) {
        // Given
        testJd.updateBookmarkStatus(!isBookmark);
        when(jdRepository.findJdWithMemberAndToDoListsById(testJd.getId())).thenReturn(Optional.of(testJd));

        // When
        BookmarkResponseDto response = jdService.updateBookmarkStatus(testJd.getId(), isBookmark, mockMember.getId());

        // Then
        assertNotNull(response);
        assertEquals(testJd.getId(), response.getJd_id());
        assertEquals(isBookmark, response.isBookmark());
        assertEquals(isBookmark, testJd.isBookmark());

        verify(jdRepository, times(1)).findJdWithMemberAndToDoListsById(testJd.getId());
    }

    @Test
    @DisplayName("북마크 상태 업데이트 실패 - JD를 찾을 수 없음")
    void updateBookmarkStatus_fail_jdNotFound() {
        // Given
        when(jdRepository.findJdWithMemberAndToDoListsById(testJd.getId())).thenReturn(Optional.empty());

        // When & Then
        JDException exception = assertThrows(JDException.class,
                () -> jdService.updateBookmarkStatus(testJd.getId(), true, mockMember.getId()));
        assertEquals(JDErrorCode.NOT_FOUND_JD, exception.getErrorCode());

        verify(jdRepository, times(1)).findJdWithMemberAndToDoListsById(testJd.getId());
        verify(jdRepository, never()).save(any(JD.class));
    }

    @Test
    @DisplayName("북마크 상태 업데이트 실패 - 권한 없음")
    void updateBookmarkStatus_fail_unauthorizedAccess() {
        // Given
        Member requestMember = Member.builder().id(200L).username("otherUser").build();

        when(jdRepository.findJdWithMemberAndToDoListsById(testJd.getId())).thenReturn(Optional.of(testJd));


        // When & Then
        JDException exception = assertThrows(JDException.class,
                () -> jdService.updateBookmarkStatus(testJd.getId(), true, requestMember.getId()));
        assertEquals(JDErrorCode.UNAUTHORIZED_ACCESS, exception.getErrorCode());

        verify(jdRepository, times(1)).findJdWithMemberAndToDoListsById(testJd.getId());
        verify(jdRepository, never()).save(any(JD.class));
    }


    @Test
    @DisplayName("지원 완료 상태 토글 성공 - null에서 현재 시간으로 변경")
    void toggleApplyStatus_success_fromNullToNow() {
        // Given
        when(jdRepository.findJdWithMemberAndToDoListsById(testJd.getId())).thenReturn(Optional.of(testJd));

        // When
        ApplyStatusResponseDto response = jdService.toggleApplyStatus(testJd.getId(), mockMember.getId());

        // Then
        assertNotNull(response);
        assertEquals(testJd.getId(), response.getJd_id());
        assertNotNull(response.getApplyAt());
        assertNotNull(testJd.getApplyAt());

        verify(jdRepository, times(1)).findJdWithMemberAndToDoListsById(testJd.getId());
    }

    @Test
    @DisplayName("지원 완료 상태 토글 성공 - 현재 시간에서 null로 변경")
    void toggleApplyStatus_success_fromNowToNull() {
        // Given
        testJd.markJdAsApplied();
        when(jdRepository.findJdWithMemberAndToDoListsById(testJd.getId())).thenReturn(Optional.of(testJd));

        // When
        ApplyStatusResponseDto response = jdService.toggleApplyStatus(testJd.getId(), mockMember.getId());

        // Then
        assertNotNull(response);
        assertEquals(testJd.getId(), response.getJd_id());
        assertNull(response.getApplyAt());
        assertNull(testJd.getApplyAt());

        verify(jdRepository, times(1)).findJdWithMemberAndToDoListsById(testJd.getId());
    }

    @Test
    @DisplayName("지원 완료 상태 토글 실패 - JD를 찾을 수 없음")
    void toggleApplyStatus_fail_jdNotFound() {
        // Given
        when(jdRepository.findJdWithMemberAndToDoListsById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        JDException exception = assertThrows(JDException.class,
                () -> jdService.toggleApplyStatus(testJd.getId(), mockMember.getId()));
        assertEquals(JDErrorCode.NOT_FOUND_JD, exception.getErrorCode());

        verify(jdRepository, times(1)).findJdWithMemberAndToDoListsById(anyLong());
        verify(jdRepository, never()).save(any(JD.class));
    }

    @Test
    @DisplayName("지원 완료 상태 토글 실패 - 권한 없음")
    void toggleApplyStatus_fail_unauthorizedAccess() {
        // Given
        Member otherMember = Member.builder().id(200L).username("otherUser").build();
        when(jdRepository.findJdWithMemberAndToDoListsById(testJd.getId())).thenReturn(Optional.of(testJd));

        // When & Then
        JDException exception = assertThrows(JDException.class,
                () -> jdService.toggleApplyStatus(testJd.getId(), otherMember.getId()));
        assertEquals(JDErrorCode.UNAUTHORIZED_ACCESS, exception.getErrorCode());

        verify(jdRepository, times(1)).findJdWithMemberAndToDoListsById(testJd.getId());
        verify(jdRepository, never()).save(any(JD.class));
    }

    @Test
    @DisplayName("메모 업데이트 실패: JD 없음")
    void updateMemo_JDNotFound() {
        // Given
        JDMemoUpdateRequestDto memoRequestDto = JDMemoUpdateRequestDto.builder()
                .memo("새로운 메모")
                .build();

        when(jdRepository.findJdWithMemberAndToDoListsById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        JDException exception = assertThrows(JDException.class,
                () -> jdService.updateMemo(testJd.getId(), memoRequestDto, mockMember.getId()));

        assertEquals(JDErrorCode.NOT_FOUND_JD, exception.getErrorCode());

        // Verify interactions
        verify(jdRepository, times(1)).findJdWithMemberAndToDoListsById(anyLong());
        verify(jdRepository, never()).save(any(JD.class));
    }

    @Test
    @DisplayName("메모 업데이트 실패: 권한 없음")
    void updateMemo_UnauthorizedAccess() {
        // Given
        JDMemoUpdateRequestDto memoRequestDto = JDMemoUpdateRequestDto.builder()
                .memo("새로운 메모")
                .build();

        Member unauthorizedMember = Member.builder()
                .id(999L) // Different ID
                .username("unauthorizedUser")
                .build();

        when(jdRepository.findJdWithMemberAndToDoListsById(testJd.getId())).thenReturn(Optional.of(testJd));

        // When & Then
        JDException exception = assertThrows(JDException.class,
                () -> jdService.updateMemo(testJd.getId(), memoRequestDto, unauthorizedMember.getId()));

        assertEquals(JDErrorCode.UNAUTHORIZED_ACCESS, exception.getErrorCode());

        // Verify interactions
        verify(jdRepository, times(1)).findJdWithMemberAndToDoListsById(testJd.getId());
    }

    @ParameterizedTest(name = "showOnly={0} 필터링 시 해당 조건의 JD만 반환된다")
    @DisplayName("JD 목록 조회 필터링: showOnly 조건에 맞는 JD만 반환")
    @MethodSource("filterTestCases")
    void getAllJds_Success_appliesShowOnlyFilter(String showOnly, JD matchingJd, JD nonMatchingJd,
                                                  java.util.function.Predicate<AllGetJDResponseDto> matcher) {
        // Given
        List<JD> filteredJds = Collections.singletonList(matchingJd);
        Page<JD> jdPage = new PageImpl<>(filteredJds, pageable, 1);

        when(jdRepository.findAllJdsByMemberIdWithToDoLists(mockMember.getId(), pageable, showOnly))
                .thenReturn(jdPage);
        when(jdRepository.getJdStats(mockMember.getId(), showOnly))
                .thenReturn(new JdStatsDto(1, 0, 0, 0, 0));
        when(memberRepository.findById(mockMember.getId())).thenReturn(Optional.of(mockMember));

        // When
        PagedJdResponseDto resultPage = jdService.getAllJds(mockMember.getId(), pageable, showOnly);

        // Then
        verify(jdRepository, times(1)).findAllJdsByMemberIdWithToDoLists(mockMember.getId(), pageable, showOnly);

        assertNotNull(resultPage);
        assertNotNull(resultPage.getJds());
        assertTrue(resultPage.getJds().stream().allMatch(matcher));
    }

    static Stream<Arguments> filterTestCases() {
        Member member = Member.builder().id(1L).username("testUser").build();

        JD alarmOnJd = JD.builder().id(101L).title("백엔드 개발자").companyName("SKC").member(member).isAlarmOn(true).build();
        JD alarmOffJd = JD.builder().id(102L).title("UX 디렉터").companyName("메리츠화재").member(member).isAlarmOn(false).build();

        JD bookmarkedJd = JD.builder().id(101L).title("백엔드 개발자").companyName("SKC").member(member).isBookmark(true).build();
        JD notBookmarkedJd = JD.builder().id(102L).title("UX 디렉터").companyName("메리츠화재").member(member).isBookmark(false).build();

        JD completedJd = JD.builder().id(101L).title("백엔드 개발자").companyName("SKC").member(member).applyAt(LocalDateTime.now()).build();
        JD notCompletedJd = JD.builder().id(102L).title("UX 디렉터").companyName("메리츠화재").member(member).build();

        return Stream.of(
                Arguments.of("alarm", alarmOnJd, alarmOffJd,
                        (java.util.function.Predicate<AllGetJDResponseDto>) AllGetJDResponseDto::isAlarmOn),
                Arguments.of("bookmark", bookmarkedJd, notBookmarkedJd,
                        (java.util.function.Predicate<AllGetJDResponseDto>) AllGetJDResponseDto::isBookmark),
                Arguments.of("completed", completedJd, notCompletedJd,
                        (java.util.function.Predicate<AllGetJDResponseDto>) (jd -> jd.getApplyAt() != null))
        );
    }
}
