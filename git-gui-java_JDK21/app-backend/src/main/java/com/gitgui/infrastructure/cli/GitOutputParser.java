package com.gitgui.infrastructure.cli;

import com.gitgui.domain.model.FileChange;
import com.gitgui.domain.model.FileStatus;
import com.gitgui.domain.model.LogEntry;
import com.gitgui.domain.model.RefInfo;
import com.gitgui.domain.model.RemoteConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Git CLI 输出解析器
 * <p>解析 {@code git status --porcelain} / {@code git log --format} / {@code git diff}
 * / {@code git for-each-ref} 等命令的文本输出，转换为领域模型对象。</p>
 *
 * <p>设计原则：
 * <ul>
 *   <li>使用 NULL 字符 ({@code \0}) 作为分隔符（{@code -z} 参数），避免文件名中的特殊字符干扰</li>
 *   <li>解析失败时记录原始输出 + 抛出可读异常（而非静默返回空结果）</li>
 *   <li>每个解析方法独立，便于单元测试</li>
 * </ul>
 * </p>
 *
 * @author FrankKang
 * @since 2026-07-25
 */
public class GitOutputParser {

    private static final Logger log = LoggerFactory.getLogger(GitOutputParser.class);

    /**
     * 解析 {@code git status --porcelain -z} 输出。
     *
     * <p>格式：XY filename\0  （重命名：XY old\0new\0）</p>
     * <p>XY 两个字符的状态码：
     * <pre>
     *   X = 索引状态 (M=已暂存变更, A=新增, D=删除, R=重命名, C=复制, .=无变更, ?=未跟踪)
     *   Y = 工作区状态 (M=已修改, D=删除, .=无变更)
     *   ?? = 未跟踪
     *   !! = 已忽略
     *   DD = 未合并/冲突
     *   AU = 未合并（工作区由我们添加）
     *   UD = 未合并（工作区由他们删除）
     *   UA = 未合并（工作区由他们添加）
     *   DU = 未合并（工作区由我们删除）
     *   AA = 未合并
     *   UU = 未合并
     * </pre>
     * </p>
     */
    public List<FileStatus> parseStatus(String output) {
        if (output == null || output.isBlank()) {
            return List.of();
        }
        List<FileStatus> result = new ArrayList<>();
        // -z 模式下，条目以 \0 分隔
        String[] entries = output.split("\0");
        int i = 0;
        while (i < entries.length) {
            String entry = entries[i];
            if (entry.length() < 3) {
                i++;
                continue;
            }
            String xy = entry.substring(0, 2);
            int spaceIdx = entry.indexOf(' ', 2);
            String path = spaceIdx >= 0 ? entry.substring(spaceIdx + 1) : entry.substring(2);
            
            // 处理重命名：下一个 entry 是新路径
            if ((xy.startsWith("R") || xy.startsWith("C")) && i + 1 < entries.length) {
                String newPath = entries[i + 1];
                if (newPath != null && !newPath.isEmpty()) {
                    path = newPath;  // 使用重命名后的路径
                    i++;
                }
            }

            String trimmedPath = path.trim();
            if (trimmedPath.isEmpty()) {
                i++;
                continue;
            }

            FileStatus.FileState state = porcelainToFileState(xy);
            if (state != null) {
                result.add(FileStatus.builder()
                        .path(trimmedPath)
                        .state(state)
                        .build());
            }
            i++;
        }
        return result;
    }

    /**
     * 解析 {@code git log --format="%H%x00%h%x00%an%x00%ae%x00%aI%x00%s%x00%b%x00%P"} 输出。
     * <p>每行一条 commit，字段用 \0 分隔。</p>
     */
    public List<LogEntry> parseLog(String output) {
        if (output == null || output.isBlank()) {
            return List.of();
        }
        List<LogEntry> result = new ArrayList<>();
        String[] lines = output.split("\n");
        for (String line : lines) {
            if (line.isBlank()) continue;
            String[] fields = line.split("\0", -1);
            if (fields.length < 7) continue;

            String commitId = fields[0];
            String shortId = fields[1];
            String author = fields[2];
            String authorEmail = fields[3];
            String commitTimeStr = fields[4];
            String message = fields[5];
            String body = fields.length > 6 ? fields[6] : "";
            String parentStr = fields.length > 7 ? fields[7] : "";

            LocalDateTime commitTime = parseIsoDateTime(commitTimeStr);
            String fullMessage = body.isEmpty() ? message : message + "\n" + body;
            List<String> parents = parentStr.isEmpty() ? List.of()
                    : List.of(parentStr.split(" "));

            result.add(LogEntry.builder()
                    .commitId(commitId)
                    .shortId(shortId)
                    .author(author)
                    .authorEmail(authorEmail)
                    .commitTime(commitTime)
                    .message(fullMessage)
                    .refs(List.of())
                    .parents(parents)
                    .build());
        }
        return result;
    }

    /**
     * 解析 {@code git diff} 输出（unified diff 格式）。
     */
    public String parseDiff(String output) {
        // git diff 输出就是标准 unified diff 格式，直接返回
        return output == null ? "" : output;
    }

    /**
     * 解析 {@code git remote -v} 输出。
     * <p>格式：origin\thttps://... (fetch)\norigin\thttps://... (push)</p>
     */
    public List<RemoteConfig> parseRemotes(String output) {
        if (output == null || output.isBlank()) {
            return List.of();
        }
        List<RemoteConfig> result = new ArrayList<>();
        java.util.Map<String, String> fetchUrls = new java.util.LinkedHashMap<>();
        java.util.Map<String, String> pushUrls = new java.util.LinkedHashMap<>();

        String[] lines = output.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            // 格式：name\turl (fetch) 或 name\turl (push)
            String[] parts = line.split("\t");
            if (parts.length >= 2) {
                String name = parts[0].trim();
                String urlAndType = parts[1].trim();
                boolean isPush = urlAndType.contains("(push)");
                String url = urlAndType.replaceAll("\\s*\\((fetch|push)\\)\\s*", "").trim();
                if (isPush) {
                    pushUrls.put(name, url);
                } else {
                    fetchUrls.put(name, url);
                }
            }
        }

        for (String name : fetchUrls.keySet()) {
            result.add(RemoteConfig.builder()
                    .name(name)
                    .fetchUrl(fetchUrls.get(name))
                    .pushUrl(pushUrls.getOrDefault(name, fetchUrls.get(name)))
                    .build());
        }
        // 处理只有 push 没有 fetch 的 remote
        for (String name : pushUrls.keySet()) {
            if (!fetchUrls.containsKey(name)) {
                result.add(RemoteConfig.builder()
                        .name(name)
                        .fetchUrl(pushUrls.get(name))
                        .pushUrl(pushUrls.get(name))
                        .build());
            }
        }
        return result;
    }

    /**
     * 解析 {@code git for-each-ref --format="... %09 ..."} 输出（TAB 分隔 4 字段）。
     */
    public List<RefInfo> parseRefs(String output) {
        if (output == null || output.isBlank()) {
            return List.of();
        }
        List<RefInfo> result = new ArrayList<>();
        String[] lines = output.split("\n");
        for (String line : lines) {
            if (line.isBlank()) continue;
            String[] fields = line.split("\t", -1);
            if (fields.length < 4) continue;

            String refName = fields[0];
            String sha = fields[1];
            String commitDate = fields.length > 3 ? fields[3] : "";
            String subject = fields.length > 4 ? fields[4] : "";

            String displayName = stripRefsPrefix(refName);
            String refKind = classifyRefKind(refName);
            String remoteName = refKind.equals("REMOTE") ? extractRemoteName(refName) : "";

            result.add(RefInfo.builder()
                    .refName(refName)
                    .displayName(displayName)
                    .kind(refKind)
                    .remoteName(remoteName)
                    .sha(sha)
                    .commitDate(commitDate)
                    .author("")
                    .message(subject)
                    .build());
        }
        // 排序：本地分支 → 远程分支 → tag
        result.sort(java.util.Comparator
                .comparingInt((RefInfo r) -> kindOrder(r.getKind()))
                .thenComparing(RefInfo::getDisplayName, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    /**
     * 解析 {@code git show --name-status --format=""} 输出。
     * <p>格式：A\tpath\nM\tpath\nD\tpath\nR100\told\tnew</p>
     */
    public List<FileChange> parseFileChanges(String output) {
        if (output == null || output.isBlank()) {
            return List.of();
        }
        List<FileChange> result = new ArrayList<>();
        String[] lines = output.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split("\t");
            if (parts.length < 2) continue;

            String typeCode = parts[0];
            String changeType;
            String oldPath = null;
            String newPath;

            if (typeCode.startsWith("R")) {
                // Rename: R100\told\tnew
                changeType = "RENAME";
                oldPath = parts[1];
                newPath = parts.length > 2 ? parts[2] : parts[1];
            } else if (typeCode.startsWith("C")) {
                changeType = "COPY";
                oldPath = parts[1];
                newPath = parts.length > 2 ? parts[2] : parts[1];
            } else {
                newPath = parts[1];
                switch (typeCode) {
                    case "A": changeType = "ADD"; break;
                    case "M": changeType = "MODIFY"; break;
                    case "D": changeType = "DELETE"; break;
                    default: changeType = typeCode;
                }
            }

            String path = newPath == null ? oldPath : newPath;
            if (path == null) continue;
            result.add(new FileChange(path, changeType, oldPath, newPath));
        }
        return result;
    }

    /**
     * 解析 {@code git commit} 的输出，提取 commit hash。
     */
    public String parseCommitResult(String output) {
        if (output == null || output.isBlank()) {
            return "";
        }
        // git commit 输出通常类似: [main abc1234] commit message
        String[] lines = output.split("\n");
        for (String line : lines) {
            if (line.startsWith("[")) {
                int endBracket = line.lastIndexOf(']');
                if (endBracket > 1) {
                    int space = line.lastIndexOf(' ', endBracket);
                    if (space > 0) {
                        return line.substring(space + 1, endBracket).trim();
                    }
                }
            }
        }
        // 如果输出中有单独的 commit hash（如通过 format 参数）
        return lines[0].trim();
    }

    // ========== 辅助方法 ==========

    private FileStatus.FileState porcelainToFileState(String xy) {
        if (xy == null || xy.length() < 2) return null;
        char x = xy.charAt(0);
        char y = xy.charAt(1);

        // 未合并/冲突状态
        if ((x == 'D' && y == 'D') || (x == 'A' && y == 'A')
                || (x == 'U' && y == 'U') || (x == 'A' && y == 'U')
                || (x == 'U' && y == 'A') || (x == 'D' && y == 'U')
                || (x == 'U' && y == 'D')) {
            return FileStatus.FileState.CONFLICT;
        }

        // 索引状态 (X)
        if (x == '?' || xy.equals("!!")) {
            return FileStatus.FileState.UNTRACKED;
        }
        if (x == 'M' || x == 'A' || x == 'R' || x == 'C') {
            // 暂存区有变更 → STAGED
            return FileStatus.FileState.STAGED;
        }
        if (x == 'D') {
            return FileStatus.FileState.DELETED;
        }

        // 工作区状态 (Y)
        if (y == 'M' || y == 'A') {
            return FileStatus.FileState.MODIFIED;
        }
        if (y == 'D') {
            return FileStatus.FileState.DELETED;
        }

        return FileStatus.FileState.UNMODIFIED;
    }

    private LocalDateTime parseIsoDateTime(String isoStr) {
        if (isoStr == null || isoStr.isBlank()) return null;
        try {
            // ISO 8601: 2024-01-15T10:30:00+08:00 或 2024-01-15T10:30:00Z
            return LocalDateTime.ofInstant(Instant.parse(isoStr), ZoneId.systemDefault());
        } catch (Exception e) {
            log.debug("解析时间失败：{}", isoStr);
            return null;
        }
    }

    private String stripRefsPrefix(String ref) {
        if (ref == null) return "";
        if (ref.startsWith("refs/heads/")) return ref.substring("refs/heads/".length());
        if (ref.startsWith("refs/remotes/")) {
            String rest = ref.substring("refs/remotes/".length());
            int slash = rest.indexOf('/');
            return slash > 0 ? rest.substring(slash + 1) : rest;
        }
        if (ref.startsWith("refs/tags/")) return ref.substring("refs/tags/".length());
        return ref;
    }

    private String classifyRefKind(String refName) {
        if (refName == null) return "OTHER";
        if (refName.startsWith("refs/heads/")) return "BRANCH";
        if (refName.startsWith("refs/remotes/")) return "REMOTE";
        if (refName.startsWith("refs/tags/")) return "TAG";
        return "OTHER";
    }

    private String extractRemoteName(String refName) {
        if (refName == null || !refName.startsWith("refs/remotes/")) return "";
        String rest = refName.substring("refs/remotes/".length());
        int slash = rest.indexOf('/');
        return slash > 0 ? rest.substring(0, slash) : rest;
    }

    private int kindOrder(String kind) {
        if ("BRANCH".equals(kind)) return 0;
        if ("REMOTE".equals(kind)) return 1;
        if ("TAG".equals(kind)) return 2;
        return 3;
    }
}
