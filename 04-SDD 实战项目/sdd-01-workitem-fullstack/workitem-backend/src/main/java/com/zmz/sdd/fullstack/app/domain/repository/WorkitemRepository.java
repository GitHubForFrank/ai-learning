package com.zmz.sdd.fullstack.app.domain.repository;

import com.zmz.sdd.fullstack.app.domain.model.Workitem;
import com.zmz.sdd.fullstack.app.domain.model.WorkitemStatus;
import com.zmz.sdd.fullstack.core.common.PageVO;

import java.util.Optional;

/** [SDD-SPEC: 03-技术方案.md §4] domain 层接口 */
public interface WorkitemRepository {
    Workitem save(Workitem workitem);
    Workitem update(Workitem workitem);
    Optional<Workitem> findById(Long id);
    PageVO<Workitem> page(int page, int size, WorkitemStatus status);
    boolean softDeleteById(Long id);
}
