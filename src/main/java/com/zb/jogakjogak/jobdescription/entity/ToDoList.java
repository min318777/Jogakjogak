package com.zb.jogakjogak.jobdescription.entity;


import com.zb.jogakjogak.global.BaseEntity;
import com.zb.jogakjogak.jobdescription.type.ToDoListType;
import jakarta.persistence.*;
import lombok.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    @Column(nullable = false, length = 50)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT", length = 1000)
    private String content;

    @Builder.Default
    @Column(columnDefinition = "VARCHAR(255) DEFAULT ''")
    private String memo = "";


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jd_id", nullable = false)
    private JD jd;

    public static ToDoList from(ToDoListType category, String title, String content, String memo,
                                boolean isDone, JD jd) {

        Logger logger = LoggerFactory.getLogger(ToDoList.class);

        if (category == null) {
            logger.warn("LLM 응답에서 ToDoList category가 누락되었습니다. 기본값으로 설정합니다.");
            category = ToDoListType.SCHEDULE_MISC_ERROR;
        }

        if (title == null || title.isEmpty()) {
            logger.warn("LLM 응답에서 ToDoList title이 누락되었습니다. 기본값으로 설정합니다.");
            title = "제목 없음";
        }

        if (content == null || content.isEmpty()) {
            logger.warn("LLM 응답에서 ToDoList content가 누락되었습니다. 기본값으로 설정합니다.");
            content = "내용 없음";
        }
        return ToDoList.builder()
                .category(category)
                .title(title)
                .content(content)
                .memo(memo)
                .isDone(isDone)
                .jd(jd)
                .build();
    }

    public static ToDoList createToDoList(ToDoListType category, String title, String content, JD jd) {
        return ToDoList.builder()
                .category(category)
                .title(title)
                .content(content)
                .memo("")
                .isDone(false)
                .jd(jd)
                .build();
    }

    public void setJd(JD jd) {
        this.jd = jd;
    }

    public void updateFromDto(ToDoListType category, String title, String content, Boolean isDone) {
        if (category != null) {
            this.category = category;
        }
        if (title != null) {
            this.title = title;
        }
        if (content != null) {
            this.content = content;
        }
        if (isDone != null) {
            this.isDone = isDone;
        }
    }

    public void updateFromBulkUpdateToDoLists(String title, String content, boolean isDone, ToDoListType category) {
        this.category = category;
        this.title = title;
        this.content = content;
        this.memo = "";
        this.isDone = isDone;
    }

    public void updateToDoListIsDone(boolean isDone){
        this.isDone = isDone;
    }
}
