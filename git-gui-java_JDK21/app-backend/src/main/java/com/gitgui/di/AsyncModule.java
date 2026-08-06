package com.gitgui.di;

import com.gitgui.core.async.TaskManager;
import com.gitgui.domain.repository.TaskRecordRepository;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

/**
 * 异步任务 Module
 * <p>绑定 {@link TaskManager} 单例。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public class AsyncModule extends AbstractModule {

    /**
     * 提供 TaskManager 单例。
     *
     * @param taskRecordRepository 任务记录仓储
     * @return TaskManager 实例
     */
    @Provides
    @Singleton
    public TaskManager provideTaskManager(TaskRecordRepository taskRecordRepository) {
        return new TaskManager(taskRecordRepository);
    }
}
