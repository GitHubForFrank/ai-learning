package com.gitgui.infrastructure.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gitgui.domain.model.FileChange;
import com.gitgui.domain.model.FileStatus;
import com.gitgui.domain.model.LogEntry;
import com.gitgui.domain.model.RefInfo;
import com.gitgui.domain.model.RemoteConfig;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * GitOutputParser 单元测试
 */
class GitOutputParserTest {

    private final GitOutputParser parser = new GitOutputParser();

    @Nested
    @DisplayName("parseStatus - git status --porcelain -z")
    class ParseStatusTests {

        @Test
        @DisplayName("解析已修改文件 (M)")
        void shouldParseModifiedFile() {
            String output = " M modified.txt\0";
            List<FileStatus> result = parser.parseStatus(output);
            assertEquals(1, result.size());
            assertEquals(FileStatus.FileState.MODIFIED, result.get(0)
                                                              .getState());
            assertEquals("modified.txt", result.get(0)
                                               .getPath());
        }

        @Test
        @DisplayName("解析未跟踪文件 (??)")
        void shouldParseUntrackedFile() {
            String output = "?? newfile.txt\0";
            List<FileStatus> result = parser.parseStatus(output);
            assertEquals(1, result.size());
            assertEquals(FileStatus.FileState.UNTRACKED, result.get(0)
                                                               .getState());
        }

        @Test
        @DisplayName("解析已暂存文件 (M 索引)")
        void shouldParseStagedFile() {
            String output = "M  staged.txt\0";
            List<FileStatus> result = parser.parseStatus(output);
            assertEquals(1, result.size());
            assertEquals(FileStatus.FileState.STAGED, result.get(0)
                                                            .getState());
        }

        @Test
        @DisplayName("解析已删除文件 (D)")
        void shouldParseDeletedFile() {
            String output = " D deleted.txt\0";
            List<FileStatus> result = parser.parseStatus(output);
            assertEquals(1, result.size());
            assertEquals(FileStatus.FileState.DELETED, result.get(0)
                                                             .getState());
        }

        @Test
        @DisplayName("解析冲突文件 (UU)")
        void shouldParseConflictFile() {
            String output = "UU conflict.txt\0";
            List<FileStatus> result = parser.parseStatus(output);
            assertEquals(1, result.size());
            assertEquals(FileStatus.FileState.CONFLICT, result.get(0)
                                                              .getState());
        }

        @Test
        @DisplayName("解析空输出")
        void shouldHandleEmptyOutput() {
            List<FileStatus> result = parser.parseStatus("");
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("解析 null 输出")
        void shouldHandleNullOutput() {
            List<FileStatus> result = parser.parseStatus(null);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("解析多个文件混合状态")
        void shouldParseMultipleFiles() {
            String output = " M modified.txt\0?? new.txt\0A  added.txt\0";
            List<FileStatus> result = parser.parseStatus(output);
            assertEquals(3, result.size());
            assertEquals(FileStatus.FileState.MODIFIED, result.get(0)
                                                              .getState());
            assertEquals(FileStatus.FileState.UNTRACKED, result.get(1)
                                                               .getState());
            assertEquals(FileStatus.FileState.STAGED, result.get(2)
                                                            .getState());
        }
    }

    @Nested
    @DisplayName("parseLog - git log --format")
    class ParseLogTests {

        @Test
        @DisplayName("解析单条日志")
        void shouldParseSingleLogEntry() {
            String output =
                    "abc1234def5678\0abc1234\0Test User\0test@test.com\0" + "2024-01-15T10:30:00+08:00\0Initial commit\0detailed body\0parentSha\n";
            List<LogEntry> result = parser.parseLog(output);
            assertEquals(1, result.size());
            LogEntry entry = result.get(0);
            assertEquals("abc1234def5678", entry.getCommitId());
            assertEquals("abc1234", entry.getShortId());
            assertEquals("Test User", entry.getAuthor());
            assertEquals("test@test.com", entry.getAuthorEmail());
            assertNotNull(entry.getCommitTime());
            assertTrue(entry.getMessage()
                            .contains("Initial commit"));
        }

        @Test
        @DisplayName("解析空输出")
        void shouldHandleEmptyLogOutput() {
            List<LogEntry> result = parser.parseLog("");
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("解析空白行")
        void shouldSkipBlankLines() {
            String output = "\n\nabc1\0abc1\0User\0a@b.c\0" + "2024-01-01T00:00:00Z\0msg\0\0\n\n";
            List<LogEntry> result = parser.parseLog(output);
            assertEquals(1, result.size());
        }
    }

    @Nested
    @DisplayName("parseRemotes - git remote -v")
    class ParseRemotesTests {

        @Test
        @DisplayName("解析单个 remote")
        void shouldParseSingleRemote() {
            String output = "origin\thttps://github.com/user/repo.git (fetch)\n" + "origin\thttps://github.com/user/repo.git (push)\n";
            List<RemoteConfig> result = parser.parseRemotes(output);
            assertEquals(1, result.size());
            assertEquals("origin", result.get(0)
                                         .getName());
            assertEquals("https://github.com/user/repo.git", result.get(0)
                                                                   .getFetchUrl());
            assertEquals("https://github.com/user/repo.git", result.get(0)
                                                                   .getPushUrl());
        }

        @Test
        @DisplayName("解析不同 fetch/push URL")
        void shouldParseDifferentFetchPushUrl() {
            String output = "origin\thttps://github.com/user/repo.git (fetch)\n" + "origin\tgit@github.com:user/repo.git (push)\n";
            List<RemoteConfig> result = parser.parseRemotes(output);
            assertEquals(1, result.size());
            assertEquals("https://github.com/user/repo.git", result.get(0)
                                                                   .getFetchUrl());
            assertEquals("git@github.com:user/repo.git", result.get(0)
                                                               .getPushUrl());
        }

        @Test
        @DisplayName("解析空输出")
        void shouldHandleEmptyRemotes() {
            List<RemoteConfig> result = parser.parseRemotes("");
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("parseRefs - git for-each-ref")
    class ParseRefsTests {

        @Test
        @DisplayName("解析分支引用")
        void shouldParseBranchRef() {
            String output = "refs/heads/main\tabc123\t2024-01-15T10:30:00+08:00\tInitial commit\n";
            List<RefInfo> result = parser.parseRefs(output);
            assertEquals(1, result.size());
            assertEquals("refs/heads/main", result.get(0)
                                                  .getRefName());
            assertEquals("main", result.get(0)
                                       .getDisplayName());
            assertEquals("BRANCH", result.get(0)
                                         .getKind());
        }

        @Test
        @DisplayName("解析 tag 引用")
        void shouldParseTagRef() {
            String output = "refs/tags/v1.0.0\tdef456\t2024-06-01T00:00:00Z\tRelease v1.0.0\n";
            List<RefInfo> result = parser.parseRefs(output);
            assertEquals(1, result.size());
            assertEquals("TAG", result.get(0)
                                      .getKind());
            assertEquals("v1.0.0", result.get(0)
                                         .getDisplayName());
        }

        @Test
        @DisplayName("解析远程引用")
        void shouldParseRemoteRef() {
            String output = "refs/remotes/origin/main\tghi789\t" + "2024-01-15T10:30:00+08:00\tRemote commit\n";
            List<RefInfo> result = parser.parseRefs(output);
            assertEquals(1, result.size());
            assertEquals("REMOTE", result.get(0)
                                         .getKind());
            assertEquals("origin", result.get(0)
                                         .getRemoteName());
        }
    }

    @Nested
    @DisplayName("parseFileChanges - git show --name-status")
    class ParseFileChangesTests {

        @Test
        @DisplayName("解析新增文件")
        void shouldParseAddedFile() {
            String output = "A\tnewfile.txt\n";
            List<FileChange> result = parser.parseFileChanges(output);
            assertEquals(1, result.size());
            assertEquals("ADD", result.get(0)
                                      .getChangeType());
            assertEquals("newfile.txt", result.get(0)
                                              .getPath());
        }

        @Test
        @DisplayName("解析修改文件")
        void shouldParseModifiedFile() {
            String output = "M\tmodified.txt\n";
            List<FileChange> result = parser.parseFileChanges(output);
            assertEquals(1, result.size());
            assertEquals("MODIFY", result.get(0)
                                         .getChangeType());
        }

        @Test
        @DisplayName("解析删除文件")
        void shouldParseDeletedFile() {
            String output = "D\tremoved.txt\n";
            List<FileChange> result = parser.parseFileChanges(output);
            assertEquals(1, result.size());
            assertEquals("DELETE", result.get(0)
                                         .getChangeType());
        }

        @Test
        @DisplayName("解析重命名文件")
        void shouldParseRenamedFile() {
            String output = "R100\toldname.txt\tnewname.txt\n";
            List<FileChange> result = parser.parseFileChanges(output);
            assertEquals(1, result.size());
            assertEquals("RENAME", result.get(0)
                                         .getChangeType());
            assertEquals("oldname.txt", result.get(0)
                                              .getOldPath());
            assertEquals("newname.txt", result.get(0)
                                              .getNewPath());
        }
    }

    @Nested
    @DisplayName("parseCommitResult - git commit output")
    class ParseCommitResultTests {

        @Test
        @DisplayName("解析标准 commit 输出")
        void shouldParseCommitOutput() {
            String output = "[main abc1234] Test commit message\n 2 files changed, 10 insertions(+)\n";
            String hash = parser.parseCommitResult(output);
            assertEquals("abc1234", hash);
        }

        @Test
        @DisplayName("解析空输出")
        void shouldHandleEmptyCommitOutput() {
            String result = parser.parseCommitResult("");
            assertEquals("", result);
        }
    }
}
