package com.zmz.sdd.fullstack.app.application.service;

import com.zmz.sdd.fullstack.app.domain.model.Workitem;
import com.zmz.sdd.fullstack.app.domain.model.WorkitemPriority;
import com.zmz.sdd.fullstack.app.domain.model.WorkitemStatus;
import com.zmz.sdd.fullstack.core.common.PageVO;

import java.time.LocalDate;

public interface WorkitemService {
    Workitem create(String title, String description, WorkitemPriority priority, LocalDate dueDate);
    Workitem update(Long id, String title, String description, WorkitemStatus status,
                WorkitemPriority priority, LocalDate dueDate);
    Workitem getById(Long id);
    PageVO<Workitem> page(int page, int size, WorkitemStatus status);
    void delete(Long id);
}
