package com.zmz.sdd.fullstack.app.api.controller.dto;

import com.zmz.sdd.fullstack.app.domain.model.WorkitemPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/** [SDD-SPEC: 02-功能规范.md §3.1] */
@Data
public class WorkitemCreateReq {

    @NotBlank(message = "is required")
    @Size(min = 1, max = 100, message = "length must be 1~100")
    private String title;

    @Size(max = 500, message = "length must be <= 500")
    private String description;

    private WorkitemPriority priority;

    private LocalDate dueDate;
}
