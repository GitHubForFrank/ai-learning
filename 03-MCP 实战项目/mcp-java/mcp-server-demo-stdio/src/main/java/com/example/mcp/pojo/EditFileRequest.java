package com.example.mcp.pojo;

import java.util.List;

/**
 * 编辑文件请求参数
 *
 * @author FrankKang
 * @since 2026-07-09
 */
public record EditFileRequest(String path, List<EditItem> edits, boolean dryRun) {

}