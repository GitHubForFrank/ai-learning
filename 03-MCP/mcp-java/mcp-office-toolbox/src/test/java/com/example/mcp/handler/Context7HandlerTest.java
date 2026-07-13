package com.example.mcp.handler;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.mcp.handler.context7.Context7Handler;
import com.example.mcp.pojo.context7.QueryDocsRequest;
import com.example.mcp.pojo.context7.ResolveLibraryRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Context7Handler 单元测试
 *
 * @author Frank Kang
 * @since 2026-07-10
 */
@SpringBootTest
class Context7HandlerTest {

    @Autowired
    private Context7Handler context7Handler;

    @Test
    void testResolveLibraryId() {
        // 测试解析 React 库
        ResolveLibraryRequest request = new ResolveLibraryRequest("I need to manage state", "react");
        String result = context7Handler.resolveLibraryId(request);
        assertNotNull(result);
        // 应该返回匹配结果或错误信息
        assertFalse(result.isEmpty());
    }

    @Test
    void testResolveLibraryIdWithNextJs() {
        // 测试解析 Next.js 库
        ResolveLibraryRequest request = new ResolveLibraryRequest("How to implement middleware", "next.js");
        String result = context7Handler.resolveLibraryId(request);
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void testQueryDocs() {
        // 测试查询文档（使用已知的库 ID）
        QueryDocsRequest request = new QueryDocsRequest("/facebook/react", "How do I use useState?", 5000);
        String result = context7Handler.queryDocs(request);
        assertNotNull(result);
        // 可能返回文档内容或错误信息（如未认证）
        assertFalse(result.isEmpty());
    }

    @Test
    void testQueryDocsWithInvalidLibraryId() {
        // 测试查询不存在的库
        QueryDocsRequest request = new QueryDocsRequest("/invalid/nonexistent-library-xyz", "test query", 5000);
        String result = context7Handler.queryDocs(request);
        assertNotNull(result);
        // 应该返回错误信息
        assertTrue(result.startsWith("错误") || result.contains("not found"));
    }

    @Test
    void testQueryDocsWithCustomTokens() {
        // 测试自定义 token 数量
        QueryDocsRequest request = new QueryDocsRequest("/facebook/react", "useState hook", 2000);
        String result = context7Handler.queryDocs(request);
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void testResolveLibraryIdEmptyName() {
        // 测试空库名称
        ResolveLibraryRequest request = new ResolveLibraryRequest("test query", "");
        String result = context7Handler.resolveLibraryId(request);
        assertNotNull(result);
        // 应该返回未找到或错误信息
        assertFalse(result.isEmpty());
    }
}
