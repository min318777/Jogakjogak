package com.zb.jogakjogak.resume.service;

import com.zb.jogakjogak.resume.repository.SkillWordRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SkillWordServiceTest {

    @Mock
    private SkillWordRepository skillWordRepository;

    @InjectMocks
    private  SkillWordService skillWordService;

    @ParameterizedTest(name = "{0} 쿼리는 정렬/중복제거/최대8개로 가공되어 반환된다")
    @DisplayName("스킬 검색 기능 성공")
    @MethodSource("autoCompleteSuccessCases")
    void autoComplete_success(String query, List<String> mockSuggestions) {
        // Given
        given(skillWordRepository.findContentsByContentLike(query)).willReturn(mockSuggestions);

        // When
        List<String> result = skillWordService.getAutocompleteSuggestions(query);

        // Then
        List<String> expectedResult = mockSuggestions.stream()
                .sorted(Comparator.naturalOrder())
                .distinct()
                .limit(8)
                .collect(Collectors.toList());
        assertThat(result, equalTo(expectedResult));
    }

    static Stream<Arguments> autoCompleteSuccessCases() {
        return Stream.of(
                Arguments.of("java", Arrays.asList("JPA", "java script", "Java", "JavaScript")),
                Arguments.of("스프링", Arrays.asList("스프링", "스프링 부트", "스프링부트"))
        );
    }

    @Test
    @DisplayName("빈 문자열 쿼리 시 빈 리스트 반환")
    void getSuggestions_whenQueryIsEmpty_returnsEmptyList() {
        // Given
        String query = "";

        // When
        List<String> result = skillWordService.getAutocompleteSuggestions(query);

        // Then
        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("null 쿼리 시 빈 리스트 반환")
    void getSuggestions_whenQueryIsNull_returnsEmptyList() {
        // Given
        String query = null;

        // When
        List<String> result = skillWordService.getAutocompleteSuggestions(query);

        // Then
        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("3자 미만 영어 쿼리 시 빈 리스트 반환 (예외 리스트 제외)")
    void getSuggestions_whenEnglishQueryIsLessThan3Chars_returnsEmptyList() {
        // Given
        String query = "ja";

        // When
        List<String> result = skillWordService.getAutocompleteSuggestions(query);

        // Then
        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("3자 미만 영어 쿼리 시 예외 리스트는 허용")
    void getSuggestions_whenEnglishQueryIsException_returnsValidList() {
        // Given
        String query = "Go";
        given(skillWordRepository.findContentsByContentLike("Go")).willReturn(List.of("Go"));

        // When
        List<String> result = skillWordService.getAutocompleteSuggestions(query);

        // Then
        assertEquals(1, result.size());
        assertEquals("Go", result.get(0));
    }

    @Test
    @DisplayName("2음절 미만 한글 쿼리 시 빈 리스트 반환")
    void getSuggestions_whenKoreanQueryIsLessThan2Chars_returnsEmptyList() {
        // Given
        String query = "가";

        // When
        List<String> result = skillWordService.getAutocompleteSuggestions(query);

        // Then
        assertEquals(0, result.size());
    }

}