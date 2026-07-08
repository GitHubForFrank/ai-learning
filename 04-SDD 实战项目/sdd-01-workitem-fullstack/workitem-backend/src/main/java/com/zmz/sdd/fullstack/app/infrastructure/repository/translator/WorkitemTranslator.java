package com.zmz.sdd.fullstack.app.infrastructure.repository.translator;

import com.zmz.sdd.fullstack.app.domain.model.Workitem;
import com.zmz.sdd.fullstack.app.domain.model.WorkitemPriority;
import com.zmz.sdd.fullstack.app.domain.model.WorkitemStatus;
import com.zmz.sdd.fullstack.app.infrastructure.dao.entity.WorkitemEntity;
import org.springframework.stereotype.Component;

@Component
public class WorkitemTranslator {

    public Workitem toModel(WorkitemEntity e) {
        if (e == null) return null;
        return Workitem.builder()
                .id(e.getId())
                .title(e.getTitle())
                .description(e.getDescription())
                .status(e.getStatus() == null ? null : WorkitemStatus.valueOf(e.getStatus()))
                .priority(e.getPriority() == null ? null : WorkitemPriority.valueOf(e.getPriority()))
                .dueDate(e.getDueDate())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    public WorkitemEntity toEntity(Workitem t) {
        if (t == null) return null;
        WorkitemEntity e = new WorkitemEntity();
        e.setId(t.getId());
        e.setTitle(t.getTitle());
        e.setDescription(t.getDescription());
        e.setStatus(t.getStatus() == null ? null : t.getStatus().name());
        e.setPriority(t.getPriority() == null ? null : t.getPriority().name());
        e.setDueDate(t.getDueDate());
        return e;
    }
}
