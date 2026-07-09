package com.example.mcp.pojo.filesystem;

/**
 * 编辑操作项，包含要查找的旧文本和替换用的新文本
 *
 * @author FrankKang
 * @since 2026-07-09
 */
public record EditItem(String oldText, String newText) {

}