package com.zmz.sdd.fullstack.app.api.controller;

import com.zmz.sdd.fullstack.app.api.controller.dto.WorkitemCreateReq;
import com.zmz.sdd.fullstack.app.api.controller.dto.WorkitemUpdateReq;
import com.zmz.sdd.fullstack.app.api.controller.dto.WorkitemVO;
import com.zmz.sdd.fullstack.app.application.service.WorkitemService;
import com.zmz.sdd.fullstack.app.domain.model.Workitem;
import com.zmz.sdd.fullstack.app.domain.model.WorkitemStatus;
import com.zmz.sdd.fullstack.core.common.PageVO;
import com.zmz.sdd.fullstack.core.common.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * [SDD-TASK: Task001]
 * [SDD-SPEC: 02-功能规范.md §3.1 ~ §3.5 + §4 BR-15 全部接口需 JWT]
 */
@RestController
@RequestMapping("/api/workitems")
@RequiredArgsConstructor
@Validated
public class WorkitemController {

    private final WorkitemService service;

    @PostMapping
    public Result<WorkitemVO> create(@Valid @RequestBody WorkitemCreateReq req) {
        Workitem t = service.create(req.getTitle(), req.getDescription(), req.getPriority(), req.getDueDate());
        return Result.success(WorkitemVO.from(t));
    }

    @GetMapping
    public Result<PageVO<WorkitemVO>> page(
            @RequestParam(required = false) WorkitemStatus status,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "must be >= 1") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "must be >= 1")
                @Max(value = 100, message = "must be <= 100") int size
    ) {
        PageVO<Workitem> p = service.page(page, size, status);
        return Result.success(PageVO.of(p.getPage(), p.getSize(), p.getTotal(),
                p.getList().stream().map(WorkitemVO::from).toList()));
    }

    @GetMapping("/{id}")
    public Result<WorkitemVO> getById(@PathVariable @Min(1) Long id) {
        return Result.success(WorkitemVO.from(service.getById(id)));
    }

    @PutMapping("/{id}")
    public Result<WorkitemVO> update(@PathVariable @Min(1) Long id, @Valid @RequestBody WorkitemUpdateReq req) {
        Workitem t = service.update(id, req.getTitle(), req.getDescription(), req.getStatus(),
                req.getPriority(), req.getDueDate());
        return Result.success(WorkitemVO.from(t));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable @Min(1) Long id) {
        service.delete(id);
        return Result.success(null);
    }
}
