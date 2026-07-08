package com.zmz.sdd.fullstack.app.infrastructure.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zmz.sdd.fullstack.app.domain.model.Workitem;
import com.zmz.sdd.fullstack.app.domain.model.WorkitemStatus;
import com.zmz.sdd.fullstack.app.domain.repository.WorkitemRepository;
import com.zmz.sdd.fullstack.app.infrastructure.dao.entity.WorkitemEntity;
import com.zmz.sdd.fullstack.app.infrastructure.dao.mapper.WorkitemMapper;
import com.zmz.sdd.fullstack.app.infrastructure.repository.translator.WorkitemTranslator;
import com.zmz.sdd.fullstack.core.common.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** [SDD-SPEC: 03-技术方案.md §4 + §5.3] */
@Repository
@RequiredArgsConstructor
public class WorkitemRepositoryImpl implements WorkitemRepository {

    private final WorkitemMapper mapper;
    private final WorkitemTranslator translator;

    @Override
    public Workitem save(Workitem workitem) {
        WorkitemEntity e = translator.toEntity(workitem);
        mapper.insert(e);
        return translator.toModel(e);
    }

    @Override
    public Workitem update(Workitem workitem) {
        WorkitemEntity e = translator.toEntity(workitem);
        mapper.updateById(e);
        // 重新加载,获取 MP 自动填充后的 updated_at
        return translator.toModel(mapper.selectById(workitem.getId()));
    }

    @Override
    public Optional<Workitem> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(translator::toModel);
    }

    @Override
    public PageVO<Workitem> page(int page, int size, WorkitemStatus status) {
        // [SDD-SPEC: 02-功能规范.md §4 BR-04] createdAt DESC
        LambdaQueryWrapper<WorkitemEntity> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(WorkitemEntity::getStatus, status.name());
        }
        wrapper.orderByDesc(WorkitemEntity::getCreatedAt);
        Page<WorkitemEntity> mpPage = new Page<>(page, size);
        Page<WorkitemEntity> result = mapper.selectPage(mpPage, wrapper);
        List<Workitem> list = result.getRecords().stream().map(translator::toModel).toList();
        return PageVO.of(page, size, result.getTotal(), list);
    }

    @Override
    public boolean softDeleteById(Long id) {
        // MP `@TableLogic` 自动转 UPDATE SET deleted=1
        return mapper.deleteById(id) > 0;
    }
}
