package com.gitgui.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gitgui.core.util.IdUtil;
import com.gitgui.domain.model.AppSettings;
import com.gitgui.domain.repository.AppSettingsRepository;
import com.gitgui.infrastructure.persistence.mapper.AppSettingsMapper;
import com.gitgui.infrastructure.persistence.mybatis.MyBatisSqlSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 应用设置仓储 MyBatis-Plus 实现
 *
 * @author FrankKang
 * @since 2026-05-27
 */
@Singleton
public class AppSettingsRepositoryImpl implements AppSettingsRepository {

    private static final Logger log = LoggerFactory.getLogger(AppSettingsRepositoryImpl.class);

    private final MyBatisSqlSessionManager sessionManager;

    @Inject
    public AppSettingsRepositoryImpl(MyBatisSqlSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public void save(AppSettings settings) {
        if (settings.getId() == null) {
            settings.setId(IdUtil.newId());
        }
        settings.setUpdatedAt(LocalDateTime.now());
        try (var session = sessionManager.openSession()) {
            AppSettingsMapper mapper = session.getMapper(AppSettingsMapper.class);
            // INSERT OR REPLACE 语义：先查是否存在，存在则更新，不存在则插入
            AppSettings existing = findByKey(settings.getKey());
            if (existing != null) {
                settings.setId(existing.getId());
                mapper.updateById(settings);
            } else {
                mapper.insert(settings);
            }
        }
    }

    @Override
    public AppSettings findByKey(String key) {
        try (var session = sessionManager.openSession()) {
            AppSettingsMapper mapper = session.getMapper(AppSettingsMapper.class);
            return mapper.selectOne(
                    new LambdaQueryWrapper<AppSettings>()
                            .eq(AppSettings::getKey, key)
            );
        }
    }

    @Override
    public List<AppSettings> findAll() {
        try (var session = sessionManager.openSession()) {
            AppSettingsMapper mapper = session.getMapper(AppSettingsMapper.class);
            return mapper.selectList(new LambdaQueryWrapper<>());
        }
    }

    @Override
    public String getValue(String key) {
        AppSettings settings = findByKey(key);
        return settings == null ? "" : settings.getValue();
    }
}
