package com.zmz.sdd.fullstack.app.infrastructure.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zmz.sdd.fullstack.app.infrastructure.dao.entity.WorkitemEntity;

/** [SDD-SPEC: 03-技术方案.md §1.1] MP BaseMapper,自动处理 logical delete */
public interface WorkitemMapper extends BaseMapper<WorkitemEntity> {
}
