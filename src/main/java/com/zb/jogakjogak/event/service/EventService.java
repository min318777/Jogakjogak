package com.zb.jogakjogak.event.service;

import com.zb.jogakjogak.event.domain.responseDto.EventResponseDto;
import com.zb.jogakjogak.event.entity.Event;
import com.zb.jogakjogak.event.repository.EventRepository;
import com.zb.jogakjogak.event.type.EventType;
import com.zb.jogakjogak.global.exception.EventErrorCode;
import com.zb.jogakjogak.global.exception.EventException;
import com.zb.jogakjogak.member.entity.Member;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;

import static com.zb.jogakjogak.global.exception.EventErrorCode.NOT_FOUND_EVENT_CODE;

@Service
@RequiredArgsConstructor
public class EventService {

    private static final int MAX_CODE_GENERATION_ATTEMPTS = 10;

    private final EventRepository eventRepository;

    /**
     * 신규 회원 이벤트 코드가 아직 발급되지 않았다면 발급합니다.
     */
    @Transactional
    public void issueNewMemberCodeIfAbsent(Member member) {
        if (eventRepository.findByMemberIdAndType(member.getId(), EventType.NEW_MEMBER).isPresent()) {
            return;
        }

        String code = null;
        for (int attempt = 0; attempt < MAX_CODE_GENERATION_ATTEMPTS; attempt++) {
            String candidate = RandomStringUtils.random(6, true, true).toUpperCase();
            if (!eventRepository.existsByCode(candidate)) {
                code = candidate;
                break;
            }
        }
        if (code == null) {
            throw new EventException(EventErrorCode.FAILED_TO_GENERATE_EVENT_CODE);
        }

        Event event = Event.builder()
                .code(code)
                .member(member)
                .type(EventType.NEW_MEMBER)
                .isFirst(true)
                .build();
        eventRepository.save(event);
    }

    /**
     * 회원이 처음 등록할 때 생성된 이벤트를 조회하는 서비스 메서드
     * 두번째 조회하는 경우 isFirst를 false로 반환
     */
    @Transactional
    public EventResponseDto getNewMemberEvent(Long memberId) {
        Event event = eventRepository.findByMemberIdAndType(memberId, EventType.NEW_MEMBER)
                .orElseThrow(() -> new EventException(NOT_FOUND_EVENT_CODE));

        if (event.getIsFirst()) {
            event.notFirst();
            return EventResponseDto.builder()
                    .id(event.getId())
                    .code(event.getCode())
                    .type(event.getType())
                    .isFirst(!event.getIsFirst())
                    .build();
        }

        return EventResponseDto.from(event);
    }
}
