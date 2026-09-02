package com.zb.jogakjogak.resume.controller;

import com.zb.jogakjogak.global.HttpApiResponse;
import com.zb.jogakjogak.resume.service.SkillWordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "이력서 관리 API", description = "이력서 등록, 수정, 조회, 삭제 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/resume/skill-word")
public class SkillWordController {

    private final SkillWordService skillWordService;

    @Operation(summary = "스킬 단어 자동완성", description = "입력한 검색어(q)로 시작하는 스킬 단어 목록을 조회합니다. 일치하는 단어가 없으면 빈 목록을 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "스킬 단어 검색 완료")
    })
    @GetMapping
    public ResponseEntity<HttpApiResponse<List<String>>> autoComplete(
            @RequestParam("q") String query) {
        return ResponseEntity.ok()
                .body(
                        new HttpApiResponse<>(
                                skillWordService.getAutocompleteSuggestions(query),
                                "스킬 단어 검색 완료",
                                HttpStatus.OK)
                );
    }
}
