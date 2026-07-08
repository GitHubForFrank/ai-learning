package com.zmz.sdd.fullstack.core.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * [SDD-TASK: Task001]
 * [SDD-SPEC: 03-技术方案.md §5.4 + 02-功能规范.md §4 BR-08]
 * 自动填充 createdAt / updatedAt(Service / Controller 层无感)
 */
@Component
public class MetaObjectHandlerConfig implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // [SDD-SPEC: 02-功能规范.md §4 BR-08] 任何修改刷新 updated_at
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }
}
