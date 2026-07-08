package com.zmz.sdd.fullstack.app.api.controller.dto;

import com.zmz.sdd.fullstack.app.domain.model.WorkitemPriority;
import com.zmz.sdd.fullstack.app.domain.model.WorkitemStatus;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/** [SDD-SPEC: 02-功能规范.md §3.4] 所有字段可选,Service 层校验"至少传一个" (BR-05) */
@Data
public class WorkitemUpdateReq {

    @Size(min = 1, max = 100, message = "length must be 1~100")
    private String title;

    @Size(max = 500, message = "length must be <= 500")
    private String description;

    private WorkitemStatus status;
    private WorkitemPriority priority;
    private LocalDate dueDate;
}
