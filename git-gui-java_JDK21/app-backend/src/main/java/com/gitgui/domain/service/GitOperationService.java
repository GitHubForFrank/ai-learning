package com.gitgui.domain.service;

import com.gitgui.core.async.ProgressCallback;
import com.gitgui.core.async.TaskHandle;
import com.gitgui.domain.model.request.CheckoutRequest;
import com.gitgui.domain.model.request.CommitRequest;
import com.gitgui.domain.model.request.PullRequest;
import com.gitgui.domain.model.request.PushRequest;

/**
 * Git 操作服务接口（核心写操作）
 * <p>所有写方法执行前由 {@code CommandInterceptor} 自动调用 {@code CommandRedLineService.check()}。</p>
 * <p>关联 BR：BR-06~BR-25。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public interface GitOperationService {

    /**
     * 提交（BR-06/BR-07）。
     *
     * @param req 提交请求
     * @return 提交哈希
     */
    String commit(CommitRequest req);

    /**
     * 推送（异步，BR-09/BR-10）。
     *
     * @param req 推送请求
     * @param cb  进度回调
     * @return 任务句柄
     */
    TaskHandle push(PushRequest req, ProgressCallback cb);

    /**
     * CLI 推送回退：当 JGit 6.9.0 在某些环境下抛 ClassCastException 等内部异常时，
     * 用系统 {@code git push} CLI 兜底。CLI 用用户已配的 git config / credentials / SSH keys，
     * 通常比 JGit 更稳定。
     *
     * <p>同步方法，调用方应自行在后台线程执行。</p>
     *
     * @param req 推送请求
     * @return CLI 输出（含推送摘要）
     * @throws com.gitgui.core.exception.GitGuiException 找不到 git / 推送失败
     */
    String pushViaCli(PushRequest req);

    /**
     * 拉取（异步）。
     *
     * @param req 拉取请求
     * @param cb  进度回调
     * @return 任务句柄
     */
    TaskHandle pull(PullRequest req, ProgressCallback cb);

    /**
     * 获取（异步）。
     *
     * @param repoPath 仓库路径
     * @param remote   远程名
     * @param branch   分支
     * @param prune    是否清理已删除远程分支引用
     * @param cb       进度回调
     * @return 任务句柄
     */
    TaskHandle fetch(String repoPath, String remote, String branch, boolean prune, ProgressCallback cb);

    /**
     * 同步（Pull + Push 一键完成）。
     *
     * @param repoPath 仓库路径
     * @param remote   远程名
     * @param branch   分支
     * @param cb       进度回调
     * @return 任务句柄
     */
    TaskHandle sync(String repoPath, String remote, String branch, ProgressCallback cb);

    /**
     * 切换分支（PRD 4.6.1）。
     * <p>校验目标分支非空后，通过 JGit 执行 checkout。</p>
     *
     * @param req 切换请求
     */
    void checkout(CheckoutRequest req);
}
