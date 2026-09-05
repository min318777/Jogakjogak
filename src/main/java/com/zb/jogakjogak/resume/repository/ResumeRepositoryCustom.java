package com.zb.jogakjogak.resume.repository;

import com.zb.jogakjogak.resume.entity.Resume;

import java.util.Optional;

public interface ResumeRepositoryCustom {

    /**
     * 이력서 ID만으로 이력서와 연관된 Member를 즉시 로딩하여 조회합니다.
     * 존재 여부(404)와 소유권(403)을 구분해서 검증할 때 사용합니다.
     */
    Optional<Resume> findResumeWithMemberById(Long resumeId);

    Optional<Resume> findResumeWithCareerAndEducationAndSkill(Long memberId);

    void deleteCareersByResumeId(Long resumeId);

    void deleteEducationsByResumeId(Long resumeId);

    void deleteSkillsByResumeId(Long resumeId);
}
