package com.gitgui.domain.service;

import com.gitgui.domain.model.RepoScanRoot;

import java.util.List;

/**
 * 扫描根目录服务接口
 * <p>负责扫描根目录的 CRUD 与扫描结果的联动：</p>
 * <ul>
 *   <li>添加根目录：upsert 后触发 {@link RepositoryService#scanMultiRepo} 异步扫描</li>
 *   <li>删除根目录：移除记录并清理缓存</li>
 *   <li>重新扫描：根据 rootPath 触发重新扫描并刷新 lastScannedAt</li>
 * </ul>
 *
 * @author FrankKang
 * @since 2026-07-24
 */
public interface RepoScanRootService {

    /**
     * 添加扫描根目录（已存在则更新 lastScannedAt），并触发异步扫描。
     *
     * @param rootPath  根目录绝对路径
     * @param alias     用户别名（可空）
     * @param scanDepth 扫描深度（1~10）
     * @return 持久化后的根目录对象
     */
    RepoScanRoot add(String rootPath, String alias, int scanDepth);

    /**
     * 重新扫描指定根目录。
     *
     * @param rootPath 根目录绝对路径
     * @return 是否触发了扫描（false 表示根目录未登记）
     */
    boolean rescan(String rootPath);

    /**
     * 重新扫描全部已启用的根目录（应用启动时恢复列表使用）。
     */
    void rescanAll();

    /**
     * 删除扫描根目录。
     *
     * @param id 主键
     */
    void remove(String id);

    /**
     * 根据根目录路径删除。
     *
     * @param rootPath 根目录绝对路径
     */
    void removeByPath(String rootPath);

    /**
     * 切换启用状态。
     *
     * @param id      主键
     * @param enabled true=启用，false=禁用
     */
    void toggleEnabled(String id, boolean enabled);

    /**
     * 更新别名与排序权重。
     *
     * @param id        主键
     * @param alias     别名
     * @param sortOrder 排序权重
     */
    void updateMeta(String id, String alias, int sortOrder);

    /**
     * 列出全部启用的扫描根目录。
     *
     * @return 启用的扫描根目录列表
     */
    List<RepoScanRoot> listEnabled();

    /**
     * 列出全部扫描根目录（含禁用）。
     *
     * @return 扫描根目录列表
     */
    List<RepoScanRoot> listAll();
}