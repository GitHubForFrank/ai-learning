package com.zmz.sdd.fullstack.app.domain.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * [SDD-SPEC: 02-功能规范.md §2.1]
 * 领域模型;不包含 deleted 字段(MP 逻辑删除字段仅 Entity 可见)
 */
@Data
@Builder
public class Workitem {
    private Long id;
    private String title;
    private String description;
    private WorkitemStatus status;
    private WorkitemPriority priority;
    private LocalDate dueDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
