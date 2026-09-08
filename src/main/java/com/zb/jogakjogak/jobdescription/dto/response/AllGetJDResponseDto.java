package com.zb.jogakjogak.jobdescription.dto.response;

import com.zb.jogakjogak.jobdescription.entity.JD;
import com.zb.jogakjogak.jobdescription.entity.ToDoList;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Schema(description = "분석 목록 조회 응답 DTO")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllGetJDResponseDto {
    @Schema(description = "분석 아이디", example = "1")
    private Long jd_id;
    @Schema(description = "채용공고 제목", example = "백엔드 신입 개발자 채용")
    private String title;
    @Schema(description = "즐겨찾기 설정 여부", example = "true")
    private boolean isBookmark;
    @Schema(description = "알림 설정 여부", example = "true")
    private boolean isAlarmOn;
    @Schema(description = "회사명", example = "주식회사 조각조각")
    private String companyName;
    @Schema(description = "전체 투두리스트 개수", example = "8")
    private Long totalPieces;
    @Schema(description = "완료된 투두리스트 개수", example = "3")
    private Long completedPieces;
    @Schema(description = "지원 완료 일시", example = "2026-09-01T12:00:00")
    private LocalDateTime applyAt;
    @Schema(description = "채용공고 마감일", example = "2026-09-30T00:00:00")
    private LocalDateTime endedAt;
    @Schema(description = "분석 생성일시", example = "2026-08-01T10:00:00")
    private LocalDateTime createdAt;
    @Schema(description = "분석 수정일시", example = "2026-08-05T10:00:00")
    private LocalDateTime updatedAt;

    /**
     * JD엔티티에서 변환합니다. 이 과정에서 JD에 연결된 ToDoList의 총 개수와 완료된 개수를 계산하여 DTO에 포함합니다.
     */
    public static AllGetJDResponseDto from(JD jd) {
        long totalPieces = jd.getToDoLists().size();
        long completedPieces = jd.getToDoLists().stream()
                .filter(ToDoList::isDone)
                .count();

        return AllGetJDResponseDto.builder()
                .jd_id(jd.getId())
                .title(jd.getTitle())
                .isBookmark(jd.isBookmark())
                .isAlarmOn(jd.isAlarmOn())
                .companyName(jd.getCompanyName())
                .completedPieces(completedPieces)
                .totalPieces(totalPieces)
                .applyAt(jd.getApplyAt())
                .createdAt(jd.getCreatedAt())
                .updatedAt(jd.getUpdatedAt())
                .endedAt(jd.getEndedAt())
                .build();
    }
}
