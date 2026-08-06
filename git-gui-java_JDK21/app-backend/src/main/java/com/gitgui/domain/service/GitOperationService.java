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
     * 同步提交（向后兼容，UI 推荐使用 {@link #commit(CommitRequest, ProgressCallback)} 异步版本以显示过程）。
     *
     * @param req 提交请求
     * @return 提交哈希
     */
    String commit(CommitRequest req);

    /**
     * 异步提交，写入异步任务队列（BR-34 同仓库写串行）。
     * <p>通过 {@link ProgressCallback} 把 git commit / pre-commit hook 的输出实时推到调用方，
     * 典型用法：UI 层把 callback 接入 {@code ProgressDialog.asCallback()}，实现「像 fetch 一样
     * 输出提交过程」的体验。</p>
     *
     * @param req 提交请求
     * @param cb  进度回调（可空）
     * @return 任务句柄
     */
    TaskHandle commit(CommitRequest req, ProgressCallback cb);

    /**
     * 推送（异步，BR-09/BR-10）。
     *
     * @param req 推送请求
     * @param cb  进度回调
     * @return 任务句柄
     */
    TaskHandle push(PushRequest req, ProgressCallback cb);

    /**
     * 同步推送（CLI 直推）。
     * <p>使用系统 {@code git push} CLI 执行，复用用户已配置的 git config / credentials / SSH keys。</p>
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
     * <p>校验目标分支非空后，通过 CLI 执行 checkout。</p>
     *
     * @param req 切换请求
     */
    void checkout(CheckoutRequest req);
}
