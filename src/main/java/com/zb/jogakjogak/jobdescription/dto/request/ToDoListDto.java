package com.zb.jogakjogak.jobdescription.dto.request;

import com.zb.jogakjogak.jobdescription.type.ToDoListType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ToDoListDto {

    private ToDoListType category;
    private String title;
    private String content;
    private String memo;
    private boolean isDone;
}
