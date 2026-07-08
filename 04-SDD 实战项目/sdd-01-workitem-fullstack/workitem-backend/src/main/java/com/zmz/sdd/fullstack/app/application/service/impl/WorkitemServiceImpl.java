package com.zmz.sdd.fullstack.app.application.service.impl;

import com.zmz.sdd.fullstack.app.application.service.WorkitemService;
import com.zmz.sdd.fullstack.app.domain.model.Workitem;
import com.zmz.sdd.fullstack.app.domain.model.WorkitemPriority;
import com.zmz.sdd.fullstack.app.domain.model.WorkitemStatus;
import com.zmz.sdd.fullstack.app.domain.repository.WorkitemRepository;
import com.zmz.sdd.fullstack.core.common.BizException;
import com.zmz.sdd.fullstack.core.common.ErrorCode;
import com.zmz.sdd.fullstack.core.common.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * [SDD-TASK: Task001 (BR-15 jwt protection only;BR-01~08 unchanged)]
 * [SDD-SPEC: 02-功能规范.md §4 BR-01 ~ BR-08]
 */
@Service
@RequiredArgsConstructor
public class WorkitemServiceImpl implements WorkitemService {

    private final WorkitemRepository repo;

    @Override
    @Transactional
    public Workitem create(String title, String description, WorkitemPriority priority, LocalDate dueDate) {
        // [SDD-SPEC: §4 BR-01] status 强制 TODO
        // [SDD-SPEC: §4 BR-02] priority 默认 MEDIUM
        Workitem workitem = Workitem.builder()
                .title(title)
                .description(description)
                .status(WorkitemStatus.TODO)
                .priority(priority == null ? WorkitemPriority.MEDIUM : priority)
                .dueDate(dueDate)
                .build();
        return repo.save(workitem);
    }

    @Override
    @Transactional
    public Workitem update(Long id, String title, String description, WorkitemStatus status,
                       WorkitemPriority priority, LocalDate dueDate) {
        // [SDD-SPEC: §4 BR-05] 至少传 1 个字段
        if (title == null && description == null && status == null && priority == null && dueDate == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "At least one field must be provided");
        }
        Workitem existing = repo.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.WORKITEM_NOT_FOUND, "Workitem not found: id=" + id));

        // [SDD-SPEC: §4 BR-06] DONE 任务禁改 priority/dueDate
        if (existing.getStatus() == WorkitemStatus.DONE && (priority != null || dueDate != null)) {
            throw new BizException(ErrorCode.WORKITEM_DONE_IMMUTABLE,
                    "Done workitem cannot change priority or dueDate");
        }
        if (title != null)       existing.setTitle(title);
        if (description != null) existing.setDescription(description);
        if (status != null)      existing.setStatus(status);
        if (priority != null)    existing.setPriority(priority);
        if (dueDate != null)     existing.setDueDate(dueDate);
        return repo.update(existing);
    }

    @Override
    public Workitem getById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.WORKITEM_NOT_FOUND, "Workitem not found: id=" + id));
    }

    @Override
    public PageVO<Workitem> page(int page, int size, WorkitemStatus status) {
        return repo.page(page, size, status);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        // [SDD-SPEC: §4 BR-07] 逻辑删除 + 幂等(已删除/不存在皆 1002)
        boolean removed = repo.softDeleteById(id);
        if (!removed) {
            throw new BizException(ErrorCode.WORKITEM_NOT_FOUND, "Workitem not found: id=" + id);
        }
    }
}
