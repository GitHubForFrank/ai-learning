package com.example.mcp.pojo.crypto;

/**
 * 随机密码生成请求参数，指定长度和字符类型。
 *
 * @param length           密码长度，默认 16
 * @param includeUppercase 是否包含大写字母，默认 true
 * @param includeLowercase 是否包含小写字母，默认 true
 * @param includeDigits    是否包含数字，默认 true
 * @param includeSpecials  是否包含特殊符号，默认 true
 * @author Frank Kang
 * @since 2026-07-12
 */
public record PasswordGenerateRequest(Integer length, Boolean includeUppercase, Boolean includeLowercase, Boolean includeDigits,
                                      Boolean includeSpecials) {

}
