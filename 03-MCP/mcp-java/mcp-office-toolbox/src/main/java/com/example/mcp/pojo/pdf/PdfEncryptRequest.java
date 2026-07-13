package com.example.mcp.pojo.pdf;

import java.util.Objects;

/**
 * PDF 加密请求参数，指定密码、权限和输出路径。
 *
 * @param fileAbsolutePath PDF 文件绝对路径
 * @param userPassword     打开密码（用户密码）
 * @param ownerPassword    权限密码（可选，不传则与打开密码相同）
 * @param targetFilePath   输出文件路径（可选）
 * @param allowPrint       是否允许打印，默认 true
 * @param allowModify      是否允许修改，默认 false
 * @param allowExtract     是否允许提取内容，默认 false
 * @author Frank Kang
 * @since 2026-07-12
 */
public record PdfEncryptRequest(String fileAbsolutePath, String userPassword, String ownerPassword, String targetFilePath, Boolean allowPrint,
                                Boolean allowModify, Boolean allowExtract) {

    public PdfEncryptRequest {
        Objects.requireNonNull(fileAbsolutePath, "fileAbsolutePath 不能为空");
        Objects.requireNonNull(userPassword, "userPassword 不能为空");
        Objects.requireNonNull(targetFilePath, "targetFilePath 不能为空");
    }
}
