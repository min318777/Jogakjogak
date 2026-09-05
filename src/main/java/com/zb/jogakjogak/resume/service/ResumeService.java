package com.zb.jogakjogak.resume.service;

import com.zb.jogakjogak.global.exception.AuthException;
import com.zb.jogakjogak.global.exception.MemberErrorCode;
import com.zb.jogakjogak.global.exception.ResumeException;
import com.zb.jogakjogak.resume.domain.requestDto.ResumeCreateRequestDtoV2;
import com.zb.jogakjogak.resume.domain.requestDto.ResumeCreateRequestDto;
import com.zb.jogakjogak.resume.domain.requestDto.ResumeUpdateRequestDto;
import com.zb.jogakjogak.resume.domain.requestDto.ResumeUpdateRequestDtoV2;
import com.zb.jogakjogak.resume.domain.responseDto.ResumeGetResponseDto;
import com.zb.jogakjogak.resume.domain.responseDto.ResumeResponseDto;
import com.zb.jogakjogak.resume.entity.Career;
import com.zb.jogakjogak.resume.entity.Education;
import com.zb.jogakjogak.resume.entity.Resume;
import com.zb.jogakjogak.resume.entity.Skill;
import com.zb.jogakjogak.resume.repository.CareerRepository;
import com.zb.jogakjogak.resume.repository.EducationRepository;
import com.zb.jogakjogak.resume.repository.ResumeRepository;
import com.zb.jogakjogak.resume.repository.SkillRepository;
import com.zb.jogakjogak.security.entity.Member;
import com.zb.jogakjogak.security.repository.MemberRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static com.zb.jogakjogak.global.exception.ResumeErrorCode.*;

@Service
@RequiredArgsConstructor
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final CareerRepository careerRepository;
    private final EducationRepository educationRepository;
    private final SkillRepository skillRepository;
    private final MemberRepository memberRepository;

    /**
     * 이력서 등록을 위한 서비스 레이어 메서드
     *
     * @param requestDto 이력서 이름, 이력서 내용
     * @return 이력서 id, 이력서 이름, 이력서 번호
     */

    @Transactional
    public ResumeResponseDto register(ResumeCreateRequestDto requestDto, Long memberId) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new AuthException(MemberErrorCode.NOT_FOUND_MEMBER));

        if (member.getResume() != null) {
            throw new AuthException(MemberErrorCode.ALREADY_HAVE_RESUME);
        }

        Resume newResume = Resume.builder()
                .title(requestDto.getTitle())
                .content(requestDto.getContent())
                .member(member)
                .build();

        Resume resume = resumeRepository.save(newResume);
        return ResumeResponseDto.builder()
                .resumeId(resume.getId())
                .title(resume.getTitle())
                .content(resume.getContent())
                .createdAt(resume.getCreatedAt())
                .updatedAt(resume.getUpdatedAt())
                .build();
    }

    @Transactional
    public ResumeResponseDto modify(Long resumeId, @Valid ResumeUpdateRequestDto requestDto, Long memberId) {

        Resume resume = getAuthorizedResume(resumeId, memberId);

        resume.modify(requestDto);
        Resume savedResume = resumeRepository.save(resume);
        return ResumeResponseDto.builder()
                .resumeId(savedResume.getId())
                .title(savedResume.getTitle())
                .content(savedResume.getContent())
                .createdAt(savedResume.getCreatedAt())
                .updatedAt(savedResume.getUpdatedAt())
                .build();
    }

    public ResumeResponseDto get(Long resumeId, Long memberId) {

        Resume resume = getAuthorizedResume(resumeId, memberId);

        return ResumeResponseDto.builder()
                .resumeId(resume.getId())
                .title(resume.getTitle())
                .content(resume.getContent())
                .createdAt(resume.getCreatedAt())
                .updatedAt(resume.getUpdatedAt())
                .build();
    }

    @Transactional
    public void delete(Long resumeId, Long memberId) {
        Resume resumeToDelete = getAuthorizedResume(resumeId, memberId);
        resumeRepository.delete(resumeToDelete);
    }

    /**
     * 이력서를 조회하고 회원이 접근 권한이 있는지 확인하는 헬퍼 메서드.
     * 존재하지 않으면 404, 본인 소유가 아니면 403을 던진다.
     */
    private Resume getAuthorizedResume(Long resumeId, Long memberId) {
        Resume resume = resumeRepository.findResumeWithMemberById(resumeId)
                .orElseThrow(() -> new ResumeException(NOT_FOUND_RESUME));
        if (!resume.getMember().getId().equals(memberId)) {
            throw new ResumeException(UNAUTHORIZED_ACCESS);
        }
        return resume;
    }


    /**
     * (v2)이력서 등록을 위한 서비스 레이어 메서드
     *
     * @param requestDto 이력서 내용, 신입 유무, 경력 리스트, 학력 리스트, 스킬 리스트
     * @return 이력서 id, 이력서 내용, 신입 유무, 경력 리스트, 학력 리스트, 스킬 리스트, 생성 일시, 수정 일시
     */
    @Transactional
    public ResumeGetResponseDto registerV2(ResumeCreateRequestDtoV2 requestDto, Long memberId) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new AuthException(MemberErrorCode.NOT_FOUND_MEMBER));

        if (member.getResume() != null) {
            throw new AuthException(MemberErrorCode.ALREADY_HAVE_RESUME);
        }

        if (!requestDto.getIsNewcomer() && requestDto.getCareerList() == null) {
            throw new ResumeException(NOT_ENTERED_CAREER);
        }

        Resume newResume = Resume.builder()
                .content(requestDto.getContent())
                .member(member)
                .isNewcomer(requestDto.getIsNewcomer())
                .build();

        member.setOnboarded(true);
        Resume saveResume = resumeRepository.save(newResume);

        return saveResumeDetails(saveResume, requestDto);
    }

    public ResumeGetResponseDto getResumeV2(Long memberId) {
        Resume resume = resumeRepository.findResumeWithCareerAndEducationAndSkill(memberId)
                .orElseThrow(() -> new ResumeException(NOT_FOUND_RESUME));

        return ResumeGetResponseDto.from(resume);
    }

    @Transactional
    public ResumeGetResponseDto modifyV2(ResumeUpdateRequestDtoV2 requestDto, Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new AuthException(MemberErrorCode.NOT_FOUND_MEMBER));
        if (member.getResume() == null) {
            throw new ResumeException(NOT_FOUND_RESUME);
        }

        Resume resume = resumeRepository.findResumeWithCareerAndEducationAndSkill(memberId)
                .orElseThrow(() -> new ResumeException(NOT_FOUND_RESUME));

        List<Career> existingCareerList = resume.getCareerList() != null ? new ArrayList<>(resume.getCareerList()) : new ArrayList<>();
        List<Education> existingEducationList = resume.getEducationList() != null ? new ArrayList<>(resume.getEducationList()) : new ArrayList<>();
        List<Skill> existingSkillList = resume.getSkillList() != null ? new ArrayList<>(resume.getSkillList()) : new ArrayList<>();

        boolean willBeNewcomer = requestDto.getIsNewcomer() != null ? requestDto.getIsNewcomer() : resume.isNewcomer();
        boolean careerWillBeEmpty = requestDto.getCareerList() != null
                ? requestDto.getCareerList().isEmpty()
                : existingCareerList.isEmpty();
        if (!willBeNewcomer && careerWillBeEmpty) {
            throw new ResumeException(NOT_ENTERED_CAREER);
        }

        resume.update(requestDto);
        resumeRepository.save(resume);

        List<Career> careerList = existingCareerList;
        if (requestDto.getCareerList() != null) {
            resumeRepository.deleteCareersByResumeId(resume.getId());
            careerList = careerRepository.saveAll(requestDto.getCareerList().stream()
                    .map(dto -> Career.of(dto, resume))
                    .toList());
        }

        List<Education> educationList = existingEducationList;
        if (requestDto.getEducationList() != null) {
            resumeRepository.deleteEducationsByResumeId(resume.getId());
            educationList = educationRepository.saveAll(requestDto.getEducationList().stream()
                    .map(dto -> Education.of(dto, resume))
                    .toList());
        }

        List<Skill> skillList = existingSkillList;
        if (requestDto.getSkillList() != null) {
            resumeRepository.deleteSkillsByResumeId(resume.getId());
            skillList = skillRepository.saveAll(requestDto.getSkillList().stream()
                    .map(content -> Skill.of(content, resume))
                    .toList());
        }

        return ResumeGetResponseDto.from(resume, careerList, educationList, skillList);
    }

    @Transactional
    public ResumeGetResponseDto saveResumeDetails(Resume resume, ResumeCreateRequestDtoV2 requestDto) {
        List<Career> savedCareerList = new ArrayList<>();
        List<Education> savedEducationList = new ArrayList<>();
        List<Skill> savedSkillList = new ArrayList<>();

        if (requestDto.getCareerList() != null) {
            List<Career> careerList = requestDto.getCareerList().stream()
                    .map(dto -> Career.of(dto, resume))
                    .toList();
            savedCareerList = careerRepository.saveAll(careerList);
        }

        if (requestDto.getEducationList() != null) {
            List<Education> educationList = requestDto.getEducationList().stream()
                    .map(dto -> Education.of(dto, resume))
                    .toList();
            savedEducationList = educationRepository.saveAll(educationList);
        }

        if (requestDto.getSkillList() != null) {
            List<Skill> skillList = requestDto.getSkillList().stream()
                    .map(dto -> Skill.of(dto, resume))
                    .toList();
            savedSkillList = skillRepository.saveAll(skillList);
        }

        return ResumeGetResponseDto.from(resume, savedCareerList, savedEducationList, savedSkillList);
    }
}
