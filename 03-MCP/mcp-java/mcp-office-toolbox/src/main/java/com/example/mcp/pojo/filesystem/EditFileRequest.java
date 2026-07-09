package com.example.mcp.pojo.filesystem;

import java.util.List;

/**
 * 编辑文件请求参数
 *
 * @author Frank Kang
 * @since 2026-07-09
 */
public record EditFileRequest(String path, List<EditItem> edits, boolean dryRun) {

}