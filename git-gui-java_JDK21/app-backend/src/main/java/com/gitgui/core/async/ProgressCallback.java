package com.gitgui.core.async;

/**
 * 进度回调接口
 * <p>异步任务执行过程中通过本接口实时反馈进度百分比与命令输出。</p>
 * <p>适配 CLI 输出解析，遵循 BR-33 进度实时反馈。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public interface ProgressCallback {

    /**
     * 空实现（用于不需要进度反馈的场景）。
     */
    ProgressCallback NOOP = new ProgressCallback() {
        @Override
        public void onProgress(int percent, String message) {
            // 空实现
        }

        @Override
        public void onOutput(String line) {
            // 空实现
        }

        @Override
        public boolean isCancelled() {
            return false;
        }
    };

    /**
     * 通知进度更新。
     *
     * @param percent 进度百分比 0-100
     * @param message 进度描述
     */
    void onProgress(int percent, String message);

    /**
     * 通知命令原始输出（用于 UI 实时显示）。
     *
     * @param line 单行输出
     */
    void onOutput(String line);

    /**
     * 检查任务是否被取消。
     * <p>CLI 任务通过轮询本方法判断是否取消。</p>
     *
     * @return true 表示用户已请求取消
     */
    boolean isCancelled();
}
