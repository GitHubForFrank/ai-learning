package com.zmz.sdd.fullstack.app.api.controller.dto;

import com.zmz.sdd.fullstack.app.domain.model.Workitem;
import com.zmz.sdd.fullstack.app.domain.model.WorkitemPriority;
import com.zmz.sdd.fullstack.app.domain.model.WorkitemStatus;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** [SDD-SPEC: 02-功能规范.md §3.1] 对外响应 DTO,不含 deleted */
@Data
public class WorkitemVO {
    private Long id;
    private String title;
    private String description;
    private WorkitemStatus status;
    private WorkitemPriority priority;
    private LocalDate dueDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static WorkitemVO from(Workitem t) {
        WorkitemVO v = new WorkitemVO();
        v.id = t.getId();
        v.title = t.getTitle();
        v.description = t.getDescription();
        v.status = t.getStatus();
        v.priority = t.getPriority();
        v.dueDate = t.getDueDate();
        v.createdAt = t.getCreatedAt();
        v.updatedAt = t.getUpdatedAt();
        return v;
    }
}
