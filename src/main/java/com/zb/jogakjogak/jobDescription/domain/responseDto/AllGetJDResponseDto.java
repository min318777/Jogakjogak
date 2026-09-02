package com.zb.jogakjogak.jobDescription.domain.responseDto;

import com.zb.jogakjogak.jobDescription.entity.JD;
import com.zb.jogakjogak.jobDescription.entity.ToDoList;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllGetJDResponseDto {
    private Long jd_id;
    private String title;
    private boolean isBookmark;
    private boolean isAlarmOn;
    private String companyName;
    private Long totalPieces;
    private Long completedPieces;
    private LocalDateTime applyAt;
    private LocalDateTime endedAt;
    private LocalDateTime createdAt;
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
