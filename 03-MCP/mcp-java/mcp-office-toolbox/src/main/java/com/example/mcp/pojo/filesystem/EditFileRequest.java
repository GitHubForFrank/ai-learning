package com.example.mcp.pojo.filesystem;

import java.util.List;
import java.util.Objects;

/**
 * 编辑文件请求参数
 *
 * @author Frank Kang
 * @since 2026-07-09
 */
public record EditFileRequest(String path, List<EditItem> edits, boolean dryRun) {

    public EditFileRequest {
        Objects.requireNonNull(path, "path 不能为空");
        Objects.requireNonNull(edits, "edits 不能为空");
    }
}