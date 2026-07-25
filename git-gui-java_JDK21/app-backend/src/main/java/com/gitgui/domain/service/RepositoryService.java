package com.gitgui.domain.service;

import com.gitgui.core.async.ProgressCallback;
import com.gitgui.core.async.TaskHandle;
import com.gitgui.domain.model.RepositoryMeta;
import com.gitgui.domain.model.request.CloneRequest;

import java.util.List;

/**
 * 仓库服务接口
 * <p>负责仓库打开、克隆、初始化、多仓库检索、元信息刷新。</p>
 * <p>关联 BR：BR-01、BR-02、BR-05、BR-41。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public interface RepositoryService {

    /**
     * 打开已有仓库。
     *
     * @param repoPath 仓库路径
     * @return 仓库元信息
     * @throws com.gitgui.core.exception.GitGuiException REPO_NOT_GIT 非 Git 目录
     */
    RepositoryMeta openRepository(String repoPath);

    /**
     * 克隆仓库（异步可取消，BR-33）。
     *
     * @param req 克隆请求
     * @param cb  进度回调
     * @return 异步任务句柄
     */
    TaskHandle clone(CloneRequest req, ProgressCallback cb);

    /**
     * 初始化仓库（git init）。
     *
     * @param dir  目标目录
     * @param bare 是否裸仓库
     */
    void initRepository(String dir, boolean bare);

    /**
     * 多仓库检索（异步可取消，BR-01/BR-02）。
     *
     * @param rootDir 根目录
     * @param depth   检索深度
     * @param cb      进度回调
     * @return 异步任务句柄
     */
    TaskHandle scanMultiRepo(String rootDir, int depth, ProgressCallback cb);

    /**
     * 刷新仓库元信息。
     *
     * @param repoPath 仓库路径
     * @return 刷新后的元信息
     */
    RepositoryMeta refreshMeta(String repoPath);

    /**
     * 获取多仓库检索结果。
     *
     * @return 检索到的仓库元信息列表
     */
    List<RepositoryMeta> getScanResults();
}
