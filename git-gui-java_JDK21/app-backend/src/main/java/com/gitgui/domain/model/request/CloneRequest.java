package com.gitgui.domain.model.request;

import lombok.Builder;
import lombok.Data;

/**
 * 克隆请求
 * <p>对应 PRD 4.1 Git Clone 对话框收集的参数。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
@Data
@Builder
public class CloneRequest {

    /** 远程仓库 URL */
    private String remoteUrl;

    /** 本地目标目录 */
    private String targetDir;

    /** 选择分支（null 表示克隆默认分支） */
    private String branch;

    /** SSH 密钥文件路径 */
    private String sshKey;

    /** 浅克隆深度（--depth，0 表示不浅克隆） */
    private int depth;

    /** 是否克隆为裸仓库（--bare） */
    private boolean bare;

    /** 是否稀疏检出 */
    private boolean sparseCheckout;

    /** 是否克隆到子目录 */
    private boolean subdirectory;
}
