package com.zb.jogakjogak.jobDescription.entity;


import com.zb.jogakjogak.global.BaseEntity;
import com.zb.jogakjogak.jobDescription.dto.request.TodoListCreateRequestDto;
import com.zb.jogakjogak.jobDescription.dto.request.ToDoListDto;
import com.zb.jogakjogak.jobDescription.dto.request.TodoListBulkItemDto;
import com.zb.jogakjogak.jobDescription.dto.request.TodoListUpdateRequestDto;
import com.zb.jogakjogak.jobDescription.type.ToDoListType;
import jakarta.persistence.*;
import lombok.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToDoList extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ToDoListType category;

    @Builder.Default
    @Column(nullable = false)
    private boolean isDone = false;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Builder.Default
    @Column(columnDefinition = "VARCHAR(255) DEFAULT ''")
    private String memo = "";


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jd_id", nullable = false)
    private JD jd;

    public static ToDoList fromDto(ToDoListDto dto, JD jd) {

        Logger logger = LoggerFactory.getLogger(ToDoList.class);

        ToDoListType category = dto.getCategory();
        if (category == null) {
            logger.warn("LLM 응답에서 ToDoList category가 누락되었습니다. 기본값으로 설정합니다.");
            category = ToDoListType.SCHEDULE_MISC_ERROR;
        }

        String title = dto.getTitle();
        if (title == null || title.isEmpty()) {
            logger.warn("LLM 응답에서 ToDoList title이 누락되었습니다. 기본값으로 설정합니다.");
            title = "제목 없음";
        }

        String content = dto.getContent();
        if (content == null || content.isEmpty()) {
            logger.warn("LLM 응답에서 ToDoList content가 누락되었습니다. 기본값으로 설정합니다.");
            content = "내용 없음";
        }
        return ToDoList.builder()
                .category(category)
                .title(title)
                .content(content)
                .memo(dto.getMemo())
                .isDone(dto.isDone())
                .jd(jd)
                .build();
    }

    public static ToDoList createToDoList(TodoListCreateRequestDto toDoListDto, JD jd) {
        return ToDoList.builder()
                .category(toDoListDto.getCategory())
                .title(toDoListDto.getTitle())
                .content(toDoListDto.getContent())
                .memo("")
                .isDone(false)
                .jd(jd)
                .build();
    }

    public void setJd(JD jd) {
        this.jd = jd;
    }

    public void updateFromDto(TodoListUpdateRequestDto dto) {
        if (dto.getCategory() != null) {
            this.category = dto.getCategory();
        }
        if (dto.getTitle() != null) {
            this.title = dto.getTitle();
        }
        if (dto.getContent() != null) {
            this.content = dto.getContent();
        }
        if (dto.getIsDone() != null) {
            this.isDone = dto.getIsDone();
        }
    }

    public void updateFromBulkUpdateToDoLists(TodoListBulkItemDto dto, ToDoListType category) {
        this.category = category;
        this.title = dto.getTitle();
        this.content = dto.getContent();
        this.memo = "";
        this.isDone = dto.isDone();
    }

    public void updateToDoListIsDone(boolean isDone){
        this.isDone = isDone;
    }
}
