package com.zb.jogakjogak.resume.service;


import com.github.javafaker.Faker;
import com.zb.jogakjogak.global.exception.AuthException;
import com.zb.jogakjogak.global.exception.MemberErrorCode;
import com.zb.jogakjogak.global.exception.ResumeErrorCode;
import com.zb.jogakjogak.global.exception.ResumeException;
import com.zb.jogakjogak.resume.dto.request.CareerDto;
import com.zb.jogakjogak.resume.dto.request.EducationDto;
import com.zb.jogakjogak.resume.dto.request.ResumeCreateRequestDtoV2;
import com.zb.jogakjogak.resume.dto.request.ResumeCreateRequestDto;
import com.zb.jogakjogak.resume.dto.request.ResumeUpdateRequestDto;
import com.zb.jogakjogak.resume.dto.request.ResumeUpdateRequestDtoV2;
import com.zb.jogakjogak.resume.dto.response.ResumeGetResponseDto;
import com.zb.jogakjogak.resume.dto.response.ResumeResponseDto;
import com.zb.jogakjogak.resume.entity.Career;
import com.zb.jogakjogak.resume.entity.Education;
import com.zb.jogakjogak.resume.entity.Resume;
import com.zb.jogakjogak.resume.entity.Skill;
import com.zb.jogakjogak.resume.repository.CareerRepository;
import com.zb.jogakjogak.resume.repository.EducationRepository;
import com.zb.jogakjogak.resume.repository.ResumeRepository;
import com.zb.jogakjogak.resume.repository.SkillRepository;
import com.zb.jogakjogak.resume.type.EducationLevel;
import com.zb.jogakjogak.resume.type.EducationStatus;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class ResumeServiceTest {

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private CareerRepository careerRepository;

    @Mock
    private EducationRepository educationRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private SkillRepository skillRepository;

    @InjectMocks
    private ResumeService resumeService;

    private Resume sampleResume;
    private ResumeCreateRequestDto sampleRequestDto;
    private Faker faker;
    private Member mockMember;


    @BeforeEach
    void setUp() {
        faker = new Faker();

        mockMember = Member.builder()
                .id(1L)
                .username("testUser")
                .email("test@example.com")
                .password("password123")
                .role(Role.USER)
                .build();


        sampleResume = Resume.builder()
                .id(1L)
                .title(faker.job().title())
                .content("기존 내용")
                .member(mockMember)
                .build();

        sampleRequestDto = ResumeCreateRequestDto.builder()
                .title("새로운 이름")
                .content("새로운 내용")
                .build();
    }

    @DisplayName("이력서 등록 실패 - 회원이 이미 이력서를 가지고 있을 때")
    @Test
    void createResume_alreadyHaveResume_throwsAuthException() {
        // Given
        String fixedUserName = "testUserWithResume";
        ResumeCreateRequestDto requestDto = ResumeCreateRequestDto.builder()
                .title("새 이력서")
                .content("새 내용")
                .build();

        Member memberWithResume = Member.builder()
                .id(2L)
                .username(fixedUserName)
                .email("test2@example.com")
                .password("pass")
                .role(Role.USER)
                .resume(Resume.builder().id(300L).build())
                .build();

        given(memberRepository.findById(2L)).willReturn(Optional.of(memberWithResume));

        // When & Then
        AuthException exception = assertThrows(AuthException.class, () -> {
            resumeService.register(requestDto, 2L);
        });

        assertThat(exception.getMemberErrorCode()).isEqualTo(MemberErrorCode.ALREADY_HAVE_RESUME);
        verify(resumeRepository, never()).save(any(Resume.class));
        verify(resumeRepository, never()).saveAndFlush(any(Resume.class));
    }

    @Test
    @DisplayName("이력서 수정 실패 테스트 - 이력서를 찾을 수 없음")
    void modify_fail_notFoundResume() {
        //Given
        Long nonExistentResumeId = 99L;
        ResumeUpdateRequestDto sampleUpdateRequestDto = ResumeUpdateRequestDto.builder()
                .title(sampleRequestDto.getTitle())
                .content(sampleRequestDto.getContent())
                .build();
        when(resumeRepository.findResumeWithMemberById(nonExistentResumeId)).thenReturn(Optional.empty());

        // When & Then
        ResumeException exception = assertThrows(ResumeException.class, () -> {
            resumeService.modify(nonExistentResumeId, sampleUpdateRequestDto, mockMember.getId());
        });

        // 예외 메시지 또는 에러 코드 검증
        assertEquals(ResumeErrorCode.NOT_FOUND_RESUME, exception.getErrorCode());

        // findById 메소드가 호출되었는지 확인
        verify(resumeRepository, times(1)).findResumeWithMemberById(nonExistentResumeId);
    }

    @Test
    @DisplayName("이력서 수정 실패 테스트 - 소유자가 아닌 회원이 접근 (IDOR 방지)")
    void modify_fail_unauthorizedAccess() {
        //Given
        Long otherMemberId = 999L;
        ResumeUpdateRequestDto sampleUpdateRequestDto = ResumeUpdateRequestDto.builder()
                .title(sampleRequestDto.getTitle())
                .content(sampleRequestDto.getContent())
                .build();
        when(resumeRepository.findResumeWithMemberById(1L)).thenReturn(Optional.of(sampleResume));

        // When & Then
        ResumeException exception = assertThrows(ResumeException.class, () ->
                resumeService.modify(1L, sampleUpdateRequestDto, otherMemberId));

        assertEquals(ResumeErrorCode.UNAUTHORIZED_ACCESS, exception.getErrorCode());
        verify(resumeRepository, times(1)).findResumeWithMemberById(1L);
        verify(resumeRepository, never()).save(any(Resume.class));
    }

    @Test
    @DisplayName("이력서 조회 실패 테스트 - 이력서를 찾을 수 없음")
    void get_fail_notFoundResume() {
        //Given
        Long nonExistentResumeId = 99L;
        when(resumeRepository.findResumeWithMemberById(nonExistentResumeId)).thenReturn(Optional.empty());

        // When & Then
        ResumeException exception = assertThrows(ResumeException.class, () -> {
            resumeService.get(nonExistentResumeId, mockMember.getId());
        });

        // 예외 메시지 또는 에러 코드 검증
        assertEquals(ResumeErrorCode.NOT_FOUND_RESUME, exception.getErrorCode());

        verify(resumeRepository, times(1)).findResumeWithMemberById(nonExistentResumeId);
    }

    @Test
    @DisplayName("이력서 조회 실패 테스트 - 소유자가 아닌 회원이 접근 (IDOR 방지)")
    void get_fail_unauthorizedAccess() {
        //Given
        Long otherMemberId = 999L;
        when(resumeRepository.findResumeWithMemberById(1L)).thenReturn(Optional.of(sampleResume));

        // When & Then
        ResumeException exception = assertThrows(ResumeException.class, () ->
                resumeService.get(1L, otherMemberId));

        assertEquals(ResumeErrorCode.UNAUTHORIZED_ACCESS, exception.getErrorCode());
        verify(resumeRepository, times(1)).findResumeWithMemberById(1L);
    }

    @DisplayName("이력서 삭제 실패 테스트 - 이력서를 찾을 수 없음")
    @Test
    void deleteResume_fail_notFound() {
        // Given
        Long nonExistentResumeId = 99L;
        given(resumeRepository.findResumeWithMemberById(nonExistentResumeId)).willReturn(Optional.empty());

        // When & Then
        ResumeException exception = assertThrows(ResumeException.class, () -> {
            resumeService.delete(nonExistentResumeId, mockMember.getId());
        });

        assertEquals(ResumeErrorCode.NOT_FOUND_RESUME, exception.getErrorCode());

        verify(resumeRepository, times(1)).findResumeWithMemberById(nonExistentResumeId);
        verify(resumeRepository, times(0)).delete(any(Resume.class));
    }

    @DisplayName("이력서 삭제 실패 테스트 - 소유자가 아닌 회원이 접근 (IDOR 방지)")
    @Test
    void deleteResume_fail_unauthorizedAccess() {
        // Given
        Long otherMemberId = 999L;
        when(resumeRepository.findResumeWithMemberById(1L)).thenReturn(Optional.of(sampleResume));

        // When & Then
        ResumeException exception = assertThrows(ResumeException.class, () ->
                resumeService.delete(1L, otherMemberId));

        assertEquals(ResumeErrorCode.UNAUTHORIZED_ACCESS, exception.getErrorCode());
        verify(resumeRepository, times(1)).findResumeWithMemberById(1L);
        verify(resumeRepository, never()).delete(any(Resume.class));
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("(v2) 이력서 추가 성공")
    @MethodSource("registerV2SuccessCases")
    void registerV2_success(String caseName, ResumeCreateRequestDtoV2 requestDto, boolean isNewcomer) {
        // Given
        String fixedUserName = "testUser123";
        Member mockMember = Member.builder()
                .id(1L)
                .username(fixedUserName)
                .email("test@example.com")
                .password("password123")
                .role(Role.USER)
                .resume(null)
                .build();

        Resume mockResume = Resume.builder()
                .id(1L)
                .content(requestDto.getContent())
                .member(mockMember)
                .isNewcomer(isNewcomer)
                .build();

        given(memberRepository.findById(1L))
                .willReturn(Optional.of(mockMember));
        given(resumeRepository.saveAndFlush(any(Resume.class))).willReturn(mockResume);

        // When
        ResumeGetResponseDto responseDto = resumeService.registerV2(requestDto, 1L);

        // Then
        assertThat(responseDto).isNotNull();
        assertThat(responseDto.getContent()).isEqualTo(requestDto.getContent());

        verify(resumeRepository, times(1)).saveAndFlush(any(Resume.class));
    }

    static Stream<Arguments> registerV2SuccessCases() {
        String content = "테스트용 이력서 본문입니다.";

        ResumeCreateRequestDtoV2 newcomerOnly = ResumeCreateRequestDtoV2.builder()
                .content(content)
                .isNewcomer(true)
                .build();

        ResumeCreateRequestDtoV2 newcomerWithEducationAndSkill = ResumeCreateRequestDtoV2.builder()
                .content(content)
                .isNewcomer(true)
                .educationList(new ArrayList<>(List.of(
                        EducationDto.builder()
                                .level(EducationLevel.HIGH_SCHOOL)
                                .majorField("조각고등학교")
                                .status(EducationStatus.GRADUATED)
                                .build(),
                        EducationDto.builder()
                                .level(EducationLevel.BACHELOR)
                                .majorField("조각대학교 조각학과")
                                .status(EducationStatus.GRADUATED)
                                .build()
                )))
                .skillList(new ArrayList<>(List.of("조각", "조가악")))
                .build();

        ResumeCreateRequestDtoV2 withCareerEducationAndSkill = ResumeCreateRequestDtoV2.builder()
                .content(content)
                .isNewcomer(false)
                .careerList(new ArrayList<>(List.of(
                        CareerDto.builder()
                                .companyName("조각조각")
                                .isWorking(true)
                                .joinedAt(LocalDate.of(2020, 1, 1))
                                .workPerformance("성과 요약")
                                .build()
                )))
                .educationList(new ArrayList<>(List.of(
                        EducationDto.builder()
                                .level(EducationLevel.HIGH_SCHOOL)
                                .majorField("조각고등학교")
                                .status(EducationStatus.GRADUATED)
                                .build(),
                        EducationDto.builder()
                                .level(EducationLevel.BACHELOR)
                                .majorField("조각대학교 조각학과")
                                .status(EducationStatus.GRADUATED)
                                .build()
                )))
                .skillList(new ArrayList<>(List.of("조각", "조가악")))
                .build();

        return Stream.of(
                Arguments.of("신입", newcomerOnly, true),
                Arguments.of("신입 - 학력,스킬 추가", newcomerWithEducationAndSkill, true),
                Arguments.of("경력 - 경력,학력,스킬 추가", withCareerEducationAndSkill, false)
        );
    }

    @Test
    @DisplayName("(v2) 이력서 추가 실패 - 경력인데 경력에 아무것도 없을 때")
    void registerV2_fail_not_entered_career() {
        String fixedUserName = "testUser123";
        String testContent = faker.lorem().sentence(3);
        boolean isNewcomer = false;

        Member mockMember = Member.builder()
                .id(1L)
                .username(fixedUserName)
                .email("test@example.com")
                .password("password123")
                .role(Role.USER)
                .resume(null)
                .build();

        ResumeCreateRequestDtoV2 requestDto = ResumeCreateRequestDtoV2.builder()
                .content(testContent)
                .isNewcomer(isNewcomer)
                .build();

        given(memberRepository.findById(1L)).willReturn(Optional.of(mockMember));

        ResumeException exception = assertThrows(ResumeException.class, () -> {
            resumeService.registerV2(requestDto, 1L);
        });

        // Then
        assertThat(exception.getErrorCode()).isEqualTo(ResumeErrorCode.NOT_ENTERED_CAREER);
        verify(resumeRepository, times(0)).save(any(Resume.class));
        verify(resumeRepository, times(0)).saveAndFlush(any(Resume.class));
    }

    @Test
    @DisplayName("(v2)이력서 조회 성공 테스트")
    void getResume_successV2() {
        Resume resume = Resume.builder()
                .content(faker.lorem().sentence(3))
                .member(mockMember)
                .isNewcomer(false)
                .careerList(new HashSet<>(List.of(
                        Career.builder()
                                .id(1L)
                                .companyName("조각조각")
                                .isWorking(true)
                                .joinedAt(LocalDate.of(2020, 1, 1))
                                .workPerformance(faker.lorem().paragraph(2))
                                .build()
                )))
                .educationList(new HashSet<>(List.of(
                        Education.builder()
                                .id(1L)
                                .level(EducationLevel.HIGH_SCHOOL)
                                .majorField("조각고등학교")
                                .status(EducationStatus.GRADUATED)
                                .build(),
                        Education.builder()
                                .id(2L)
                                .level(EducationLevel.BACHELOR)
                                .majorField("조각대학교 조각학과")
                                .status(EducationStatus.GRADUATED)
                                .build()
                )))
                .skillList(new HashSet<>(List.of(
                        Skill.builder()
                                .id(1L)
                                .content("조각")
                                .build()
                )))
                .build();

        //Given
        when(resumeRepository.findResumeWithCareerAndEducationAndSkill(mockMember.getId()))
                .thenReturn(Optional.of(resume));

        //When
        ResumeGetResponseDto result = resumeService.getResumeV2(mockMember.getId());

        //Then
        verify(resumeRepository, times(1)).findResumeWithCareerAndEducationAndSkill(mockMember.getId());

        assertNotNull(result);
        assertEquals(resume.getContent(), result.getContent());
        assertEquals(resume.getMember(), mockMember);
        assertEquals("조각", result.getSkillList().get(0));

        Optional<Education> filterEducation = resume.getEducationList().stream()
                .filter(education -> Objects.equals(education.getMajorField(), "조각고등학교"))
                .findFirst();

        Optional<EducationDto> filterEducationFromResult = result.getEducationDtoList().stream()
                .filter(education -> Objects.equals(education.getMajorField(), "조각고등학교"))
                .findFirst();

        assertTrue(filterEducation.isPresent());
        assertTrue(filterEducationFromResult.isPresent());
        assertEquals(filterEducation.get().getLevel(), filterEducationFromResult.get().getLevel());
        assertEquals(filterEducation.get().getMajorField(), filterEducationFromResult.get().getMajorField());
        assertEquals(filterEducation.get().getStatus(), filterEducationFromResult.get().getStatus());
    }

    @Test
    @DisplayName("(v2)이력서 조회 성공 테스트 - 신입이고 이력서 상세가 아무것도 없을 경우")
    void getResume_successV2_2() {
        Resume resume = Resume.builder()
                .content(null)
                .member(mockMember)
                .isNewcomer(true)
                .build();

        //Given
        when(resumeRepository.findResumeWithCareerAndEducationAndSkill(mockMember.getId()))
                .thenReturn(Optional.of(resume));

        //When
        ResumeGetResponseDto result = resumeService.getResumeV2(mockMember.getId());

        //Then
        verify(resumeRepository, times(1)).findResumeWithCareerAndEducationAndSkill(mockMember.getId());

        assertNotNull(result);
        assertEquals(resume.getContent(), result.getContent());
        assertEquals(resume.getMember(), mockMember);
        assertTrue(result.isNewcomer());
    }

    @Test
    @DisplayName("(v2)이력서 수정 성공 테스트")
    void modifyV2_success() {
        //Given
        Resume resume = Resume.builder()
                .content(faker.lorem().sentence(3))
                .member(mockMember)
                .isNewcomer(false)
                .careerList(new HashSet<>(List.of(
                        Career.builder()
                                .id(1L)
                                .companyName("조각조각")
                                .isWorking(true)
                                .joinedAt(LocalDate.of(2020, 1, 1))
                                .workPerformance(faker.lorem().paragraph(2))
                                .build()
                )))
                .educationList(new HashSet<>(List.of(
                        Education.builder()
                                .id(1L)
                                .level(EducationLevel.HIGH_SCHOOL)
                                .majorField("조각고등학교")
                                .status(EducationStatus.GRADUATED)
                                .build(),
                        Education.builder()
                                .id(2L)
                                .level(EducationLevel.BACHELOR)
                                .majorField("조각대학교 조각학과")
                                .status(EducationStatus.GRADUATED)
                                .build()
                )))
                .skillList(new HashSet<>(List.of(
                        Skill.builder()
                                .id(1L)
                                .content("조각")
                                .build()
                )))
                .build();

        ResumeUpdateRequestDtoV2 requestDto = ResumeUpdateRequestDtoV2.builder()
                .content(faker.lorem().sentence(3))
                .isNewcomer(false)
                .careerList(new ArrayList<>(List.of(
                        CareerDto.builder()
                                .companyName("조각조각update")
                                .isWorking(false)
                                .joinedAt(LocalDate.of(2020, 1, 1))
                                .quitAt(LocalDate.of(2020, 1, 1))
                                .workPerformance(faker.lorem().paragraph(2))
                                .build()
                )))
                .educationList(new ArrayList<>(List.of(
                        EducationDto.builder()
                                .level(EducationLevel.HIGH_SCHOOL)
                                .majorField("조각고등학교update")
                                .status(EducationStatus.GRADUATED)
                                .build(),
                        EducationDto.builder()
                                .level(EducationLevel.BACHELOR)
                                .majorField("조각대학교 조각학과update")
                                .status(EducationStatus.GRADUATED)
                                .build()
                )))
                .skillList(new ArrayList<>(List.of(
                        "조각update", "조가악update"
                )))
                .build();
        mockMember.setResume(resume);
        when(memberRepository.findById(mockMember.getId())).thenReturn(Optional.of(mockMember));
        when(resumeRepository.findResumeWithCareerAndEducationAndSkill(mockMember.getId())).thenReturn(Optional.of(resume));
        when(careerRepository.saveAll(any())).thenReturn(List.of(
                Career.builder()
                        .id(1L)
                        .companyName("조각조각update")
                        .isWorking(true)
                        .joinedAt(LocalDate.of(2020, 1, 1))
                        .workPerformance(faker.lorem().paragraph(2))
                        .build()));
        when(educationRepository.saveAll(any())).thenReturn(List.of(
                Education.builder()
                        .id(1L)
                        .level(EducationLevel.HIGH_SCHOOL)
                        .majorField("조각고등학교update")
                        .status(EducationStatus.GRADUATED)
                        .build(),
                Education.builder()
                        .id(2L)
                        .level(EducationLevel.BACHELOR)
                        .majorField("조각대학교 조각학과update")
                        .status(EducationStatus.GRADUATED)
                        .build()));
        when(skillRepository.saveAll(any())).thenReturn(List.of(
                Skill.builder()
                        .id(1L)
                        .content("조각update")
                        .build(),
                Skill.builder()
                        .id(2L)
                        .content("조가악update")
                        .build()));

        //When
        ResumeGetResponseDto result = resumeService.modifyV2(requestDto, mockMember.getId());

        //Then
        verify(resumeRepository, times(1)).findResumeWithCareerAndEducationAndSkill(mockMember.getId());

        assertNotNull(result);
        assertEquals(requestDto.getContent(), result.getContent());
        assertEquals(requestDto.getEducationList().get(0).getMajorField(), result.getEducationDtoList().get(0).getMajorField());
        assertEquals(requestDto.getSkillList().get(0), result.getSkillList().get(0));
        assertEquals(requestDto.getCareerList().get(0).getCompanyName(), result.getCareerDtoList().get(0).getCompanyName());
    }


    @Test
    @DisplayName("(v2)이력서 수정 실패 테스트 - resume을 찾을 수 없음")
    void modifyV2_failure1() {
        //Given
        ResumeUpdateRequestDtoV2 requestDto = ResumeUpdateRequestDtoV2.builder()
                .content(faker.lorem().sentence(3))
                .isNewcomer(false)
                .careerList(new ArrayList<>(List.of(
                        CareerDto.builder()
                                .companyName("조각조각update")
                                .isWorking(true)
                                .joinedAt(LocalDate.of(2020, 1, 1))
                                .workPerformance(faker.lorem().paragraph(2))
                                .build()
                )))
                .educationList(new ArrayList<>(List.of(
                        EducationDto.builder()
                                .level(EducationLevel.HIGH_SCHOOL)
                                .majorField("조각고등학교update")
                                .status(EducationStatus.GRADUATED)
                                .build(),
                        EducationDto.builder()
                                .level(EducationLevel.BACHELOR)
                                .majorField("조각대학교 조각학과update")
                                .status(EducationStatus.GRADUATED)
                                .build()
                )))
                .skillList(new ArrayList<>(List.of(
                        "조각update", "조가악update"
                )))
                .build();

        given(memberRepository.findById(mockMember.getId())).willReturn(Optional.of(mockMember));

        //When
        ResumeException exception = assertThrows(ResumeException.class, () ->
                resumeService.modifyV2(requestDto, mockMember.getId()));

        //Then
        verify(resumeRepository, times(0)).findResumeWithCareerAndEducationAndSkill(mockMember.getId());
        assertThat(exception.getErrorCode()).isEqualTo(ResumeErrorCode.NOT_FOUND_RESUME);
    }

    @Test
    @DisplayName("(v2)이력서 수정 실패 테스트 - 신입이 아닌데 경력을 작성안함")
    void modifyV2_failure2() {
        //Given
        Resume resume = Resume.builder()
                .content(faker.lorem().sentence(3))
                .member(mockMember)
                .isNewcomer(false)
                .careerList(new HashSet<>(List.of(
                        Career.builder()
                                .id(1L)
                                .companyName("조각조각")
                                .isWorking(true)
                                .joinedAt(LocalDate.of(2020, 1, 1))
                                .workPerformance(faker.lorem().paragraph(2))
                                .build()
                )))
                .educationList(new HashSet<>(List.of(
                        Education.builder()
                                .id(1L)
                                .level(EducationLevel.HIGH_SCHOOL)
                                .majorField("조각고등학교")
                                .status(EducationStatus.GRADUATED)
                                .build(),
                        Education.builder()
                                .id(2L)
                                .level(EducationLevel.BACHELOR)
                                .majorField("조각대학교 조각학과")
                                .status(EducationStatus.GRADUATED)
                                .build()
                )))
                .skillList(new HashSet<>(List.of(
                        Skill.builder()
                                .id(1L)
                                .content("조각")
                                .build()
                )))
                .build();
        mockMember.setResume(resume);
        when(memberRepository.findById(mockMember.getId())).thenReturn(Optional.of(mockMember));
        when(resumeRepository.findResumeWithCareerAndEducationAndSkill(mockMember.getId())).thenReturn(Optional.of(resume));

        ResumeUpdateRequestDtoV2 requestDto = ResumeUpdateRequestDtoV2.builder()
                .content(faker.lorem().sentence(3))
                .isNewcomer(false)
                .careerList(new ArrayList<>())
                .educationList(new ArrayList<>(List.of(
                        EducationDto.builder()
                                .level(EducationLevel.HIGH_SCHOOL)
                                .majorField("조각고등학교update")
                                .status(EducationStatus.GRADUATED)
                                .build(),
                        EducationDto.builder()
                                .level(EducationLevel.BACHELOR)
                                .majorField("조각대학교 조각학과update")
                                .status(EducationStatus.GRADUATED)
                                .build()
                )))
                .skillList(new ArrayList<>(List.of(
                        "조각update", "조가악update"
                )))
                .build();

        //When
        ResumeException exception = assertThrows(ResumeException.class, () ->
                resumeService.modifyV2(requestDto, mockMember.getId()));

        //Then
        verify(resumeRepository, times(1)).findResumeWithCareerAndEducationAndSkill(mockMember.getId());
        assertThat(exception.getErrorCode()).isEqualTo(ResumeErrorCode.NOT_ENTERED_CAREER);
    }

}
