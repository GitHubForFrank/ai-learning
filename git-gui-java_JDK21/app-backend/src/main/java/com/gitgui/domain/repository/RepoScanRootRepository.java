package com.gitgui.domain.repository;

import com.gitgui.domain.model.RepoScanRoot;

import java.util.List;

/**
 * 扫描根目录仓储接口
 * <p>领域层定义的持久化契约，由基础设施层 {@code infrastructure/persistence/repository/} 实现。</p>
 *
 * @author FrankKang
 * @since 2026-07-24
 */
public interface RepoScanRootRepository {

    /**
     * 保存（新增或更新，upsert 语义）。
     *
     * @param root 扫描根目录
     */
    void save(RepoScanRoot root);

    /**
     * 根据 ID 查找。
     *
     * @param id 主键
     * @return 扫描根目录，不存在返回 null
     */
    RepoScanRoot findById(String id);

    /**
     * 根据根目录路径查找。
     *
     * @param rootPath 根目录绝对路径
     * @return 扫描根目录，不存在返回 null
     */
    RepoScanRoot findByRootPath(String rootPath);

    /**
     * 列出全部启用的扫描根目录（按 sortOrder 升序、lastScannedAt 倒序）。
     *
     * @return 启用的扫描根目录列表
     */
    List<RepoScanRoot> findEnabled();

    /**
     * 列出全部扫描根目录（含禁用的，按 sortOrder 升序、lastScannedAt 倒序）。
     *
     * @return 全部扫描根目录列表
     */
    List<RepoScanRoot> findAll();

    /**
     * 根据 ID 删除。
     *
     * @param id 主键
     */
    void deleteById(String id);

    /**
     * 根据根目录路径删除。
     *
     * @param rootPath 根目录绝对路径
     */
    void deleteByRootPath(String rootPath);

    /**
     * 判断根目录路径是否已存在。
     *
     * @param rootPath 根目录绝对路径
     * @return true 表示已存在
     */
    boolean existsByRootPath(String rootPath);
}