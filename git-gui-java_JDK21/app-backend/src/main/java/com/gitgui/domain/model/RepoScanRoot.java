package com.gitgui.domain.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 多仓库扫描根目录领域模型
 * <p>记录用户选择的根目录及其扫描配置，用于下次启动自动恢复左侧仓库列表。</p>
 * <p>状态机：新增 → 启用 / 禁用 → 删除（不可恢复）。</p>
 * <p>关联 BR：BR-01（多仓库检索）、BR-02（异步可取消）。</p>
 *
 * @author FrankKang
 * @since 2026-07-24
 */
@Data
@Builder
@TableName("repo_scan_root")
public class RepoScanRoot {

    /** 主键 UUID */
    @TableId
    private String id;

    /** 根目录绝对路径，唯一 */
    private String rootPath;

    /** 用户自定义别名（便于在侧边栏识别，最大 100 字符） */
    private String alias;

    /** 扫描深度（1~10，BR-01 默认 3） */
    private int scanDepth;

    /** 最后一次扫描时间 */
    private LocalDateTime lastScannedAt;

    /** 是否启用（true=启用，false=禁用） */
    private boolean enabled;

    /** 排序权重（数值越小越靠前） */
    private int sortOrder;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}