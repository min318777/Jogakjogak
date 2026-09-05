package com.zb.jogakjogak.jobDescription.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.zb.jogakjogak.event.entity.Event;
import com.zb.jogakjogak.event.repository.EventRepository;
import com.zb.jogakjogak.event.type.EventType;
import com.zb.jogakjogak.global.exception.*;
import com.zb.jogakjogak.jobDescription.domain.requestDto.*;
import com.zb.jogakjogak.jobDescription.domain.responseDto.*;
import com.zb.jogakjogak.jobDescription.entity.JD;
import com.zb.jogakjogak.jobDescription.entity.ToDoList;
import com.zb.jogakjogak.jobDescription.repository.JDRepository;
import com.zb.jogakjogak.security.entity.Member;
import com.zb.jogakjogak.security.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JDService {

    private final OpenAIResponseService openAIResponseService;
    private final ObjectMapper objectMapper;
    private final JDRepository jdRepository;
    private final MemberRepository memberRepository;
    private final EventRepository eventRepository;
    private final LLMService llmService;

    /**
     * open ai를 이용하여 JD와 이력서를 분석하여 To Do List를 만들어주는 서비스 메서드
     */
    public JDResponseDto analyze(JDCreateRequestDto jdRequestDto, Long memberId) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new AuthException(MemberErrorCode.NOT_FOUND_MEMBER));

        if (member.getResume().getContent() == null) {
            throw new ResumeException(ResumeErrorCode.NOT_FOUND_RESUME);
        }

        String analysisJsonString = openAIResponseService.sendRequest(member.getResume().getContent(), jdRequestDto.getContent(), 4000);
        List<ToDoListDto> parsedAnalysisResult;
        try {
            CollectionType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, ToDoListDto.class);
            parsedAnalysisResult = objectMapper.readValue(analysisJsonString, listType);
        } catch (JsonProcessingException e) {
            throw new JDException(JDErrorCode.FAILED_JSON_PROCESS);
        }

        JD jd = JD.builder()
                .title(jdRequestDto.getTitle())
                .jdUrl(jdRequestDto.getJdUrl())
                .endedAt(jdRequestDto.getEndedAt())
                .memo("")
                .companyName(jdRequestDto.getCompanyName())
                .job(jdRequestDto.getJob())
                .content(jdRequestDto.getContent())
                .isAlarmOn(false)
                .build();

        for (ToDoListDto dto : parsedAnalysisResult) {
            ToDoList toDoList = ToDoList.fromDto(dto, jd);
            jd.addToDoList(toDoList);
        }

        JD savedJd = jdRepository.save(jd);

        return JDResponseDto.from(savedJd, member);
    }

    /**
     * gemini ai를 이용하여 JD와 이력서를 분석하여 To Do List를 만들어주는 서비스 메서드
     */
    @Transactional
    public JDResponseDto llmAnalyze(JDCreateRequestDto jdRequestDto, Long memberId) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new AuthException(MemberErrorCode.NOT_FOUND_MEMBER));

        long jdCount = jdRepository.findAllJdCountByMemberId(member.getId());

        boolean hasResume = member.getResume() != null && member.getResume().getContent() != null;
        if (!hasResume) {
            if (jdCount >= 1) {
                throw new ResumeException(ResumeErrorCode.ANALYSIS_ALLOWED_ONCE_WITHOUT_RESUME);
            }
        } else {
            jdRepository.deleteAllByMemberAndIsCreatedWithResumeFalse(member);
            if (jdCount >= 20) {
                throw new JDException(JDErrorCode.JD_LIMIT_EXCEEDED);
            }
        }

        String resumeContent = hasResume ? member.getResume().getContent() : "";
        String analysisJsonString = llmService.generateTodoListJson(resumeContent, jdRequestDto.getContent(), jdRequestDto.getJob());
        List<ToDoListDto> parsedAnalysisResult;
        try {
            parsedAnalysisResult = objectMapper.readValue(analysisJsonString, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new JDException(JDErrorCode.FAILED_JSON_PROCESS);
        }
        JD jd = createdJd(jdRequestDto, member, hasResume);
        for (ToDoListDto dto : parsedAnalysisResult) {
            ToDoList toDoList = ToDoList.fromDto(dto, jd);
            jd.addToDoList(toDoList);
        }
        JD savedJd = jdRepository.save(jd);

        // 이벤트 코드 추가
        Optional<Event> findEvent = eventRepository.findByMemberIdAndType(member.getId(), EventType.NEW_MEMBER);
        if (jdCount == 0 && findEvent.isEmpty()) {
            String code;
            while (true) {
                code = RandomStringUtils.random(6, true, true).toUpperCase();
                boolean isExists = eventRepository.existsByCode(code);
                if (!isExists) {
                    break;
                }
            }
            Event event = Event.builder()
                    .code(code)
                    .member(member)
                    .type(EventType.NEW_MEMBER)
                    .isFirst(true)
                    .build();
            eventRepository.save(event);
        }

        return JDResponseDto.from(savedJd, member);
    }

    private JD createdJd(JDCreateRequestDto jdRequestDto, Member member, boolean isCreatedWithResume) {
        return JD.builder()
                .title(jdRequestDto.getTitle())
                .isBookmark(false)
                .isAlarmOn(false)
                .isCreatedWithResume(isCreatedWithResume)
                .companyName(jdRequestDto.getCompanyName())
                .job(jdRequestDto.getJob())
                .content(jdRequestDto.getContent())
                .jdUrl(jdRequestDto.getJdUrl())
                .endedAt(jdRequestDto.getEndedAt())
                .memo("")
                .member(member)
                .build();
    }

    public JDResponseDto getJd(Long jdId, Long memberId) {

        JD jd = getAuthorizedJd(jdId, memberId);
        return JDResponseDto.from(jd, jd.getMember());
    }

    @Transactional
    public void deleteJd(Long jdId, Long memberId) {

        JD jd = getAuthorizedJd(jdId, memberId);
        jdRepository.deleteById(jd.getId());
    }

    @Transactional
    public JDAlarmResponseDto alarm(Long jdId, JDAlarmUpdateRequestDto dto, Long memberId) {

        JD jd = getAuthorizedJd(jdId, memberId);
        if (dto.isAlarmOn()) {
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new AuthException(MemberErrorCode.NOT_FOUND_MEMBER));
            member.setNotificationEnabled(true);
            memberRepository.save(member);
        }
        jd.isAlarmOn(dto.isAlarmOn());
        return JDAlarmResponseDto.builder()
                .isAlarmOn(jd.isAlarmOn())
                .jdId(jd.getId())
                .build();
    }

    /**
     * 특정 사용자의 모든 JD (Job Description) 목록을 페이징하여 조회합니다.
     *
     * @param member   조회할 사용자.
     * @param pageable 페이징 및 정렬 정보를 담는 객체.
     * @return 페이징처리된 목록을 포함하는 객체.
     * @throws AuthException 회원을 찾을 수 없을 경우 발생하는 예외.
     */

    @Transactional(readOnly = true)
    public PagedJdResponseDto getAllJds(Long memberId,
                                        Pageable pageable,
                                        String showOnly) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new AuthException(MemberErrorCode.NOT_FOUND_MEMBER));
        Page<JD> jdEntitiesPage = jdRepository.findAllJdsByMemberIdWithToDoLists(member.getId(), pageable, showOnly);
        int applyJdCount = 0, completedPiecesCount = 0, totalPiecesCount = 0, perfectJdCount = 0;

        for (JD jd : jdEntitiesPage.getContent()) {
            if (jd.getApplyAt() != null) {
                applyJdCount++;
            }

            int totalCount = jd.getToDoLists().size();
            totalPiecesCount += totalCount;
            int completedCount = (int) jd.getToDoLists().stream()
                    .filter(ToDoList::isDone)
                    .count();
            completedPiecesCount += completedCount;

            if (completedCount == totalCount) {
                perfectJdCount++;
            }
        }

        List<AllGetJDResponseDto> dtos = jdEntitiesPage.getContent().stream()
                .map(AllGetJDResponseDto::from)
                .collect(Collectors.toList());
        int allJdCount = dtos.size();

        Page<AllGetJDResponseDto> page = new PageImpl<>(dtos, pageable, jdEntitiesPage.getTotalElements());

        return new PagedJdResponseDto(page, member, allJdCount, applyJdCount,
                completedPiecesCount, totalPiecesCount, perfectJdCount);
    }

    @Transactional
    public BookmarkResponseDto updateBookmarkStatus(Long jdId, JDBookmarkUpdateRequestDto dto, Long memberId) {

        JD jd = getAuthorizedJd(jdId, memberId);

        jd.updateBookmarkStatus(dto.isBookmark());
        return BookmarkResponseDto.builder()
                .jd_id(jdId)
                .isBookmark(jd.isBookmark())
                .build();
    }

    @Transactional
    public ApplyStatusResponseDto toggleApplyStatus(Long jdId, Long memberId) {

        JD updateJd = getAuthorizedJd(jdId, memberId);
        if (updateJd.getApplyAt() == null) {
            updateJd.markJdAsApplied();
        } else {
            updateJd.unMarkJdAsApplied();
        }
        return ApplyStatusResponseDto.builder()
                .jd_id(jdId)
                .applyAt(updateJd.getApplyAt())
                .build();
    }

    @Transactional
    public MemoResponseDto updateMemo(Long jdId, JDMemoUpdateRequestDto dto, Long memberId) {
        JD jd = getAuthorizedJd(jdId, memberId);
        jd.updateMemo(dto);
        return MemoResponseDto.builder()
                .jd_id(jd.getId())
                .memo(jd.getMemo())
                .build();
    }

    @Transactional
    public JDResponseDto updateJd(Long jdId, JDUpdateRequestDto jdUpdateRequestDto, Long memberId) {
        JD jd = getAuthorizedJd(jdId, memberId);
        jd.updateJd(jdUpdateRequestDto);
        return JDResponseDto.from(jd, jd.getMember());
    }

    /**
     * Helper method to retrieve a JD and ensure the member has access.
     * JD를 검색하고 회원이 접근 권한이 있는지 확인하는 헬퍼 메서드.
     */
    private JD getAuthorizedJd(Long jdId, Long memberId) {
        JD jd = jdRepository.findJdWithMemberAndToDoListsById(jdId)
                .orElseThrow(() -> new JDException(JDErrorCode.NOT_FOUND_JD));
        if (!jd.getMember().getId().equals(memberId)) {
            throw new JDException(JDErrorCode.UNAUTHORIZED_ACCESS);
        }
        return jd;
    }
}
