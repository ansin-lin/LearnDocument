# 账票文件交付参考实现

本参考实现对应[第5章](../05_report_delivery_and_batch.md)。课堂只理解下载、保存和 Batch 边界；这里保存临时文件、原子发布、SHA-256 和路径校验示例。

## ReportFileStore.java

```java
package com.example.employee.report.file;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

public final class ReportFileStore {
    private final Path root;

    public ReportFileStore(Path configuredRoot) throws IOException {
        Files.createDirectories(configuredRoot);
        this.root = configuredRoot.toRealPath();
    }

    public PendingFile createPending(String extension) {
        if (!extension.matches("\\.[a-z0-9]{1,8}")) {
            throw new IllegalArgumentException("非法扩展名");
        }
        String storageKey = UUID.randomUUID() + extension;
        Path temporary = root.resolve(storageKey + ".part").normalize();
        Path published = root.resolve(storageKey).normalize();
        requireInsideRoot(temporary);
        requireInsideRoot(published);
        return new PendingFile(storageKey, temporary, published);
    }

    public OutputStream openForWrite(PendingFile pending)
            throws IOException {
        requireInsideRoot(pending.temporary());
        return Files.newOutputStream(pending.temporary(),
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE);
    }

    public StoredFile publish(PendingFile pending) throws IOException {
        requireInsideRoot(pending.temporary());
        requireInsideRoot(pending.published());
        try {
            Files.move(pending.temporary(), pending.published(),
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(pending.temporary(), pending.published());
        }
        return new StoredFile(pending.storageKey(),
                Files.size(pending.published()),
                sha256(pending.published()));
    }

    public Path resolveForRead(String storageKey) throws IOException {
        if (!storageKey.matches("[0-9a-fA-F-]{36}\\.[a-z0-9]{1,8}")) {
            throw new IllegalArgumentException("非法存储键");
        }
        Path candidate = root.resolve(storageKey).normalize();
        requireInsideRoot(candidate);
        Path real = candidate.toRealPath();
        requireInsideRoot(real);
        if (!Files.isRegularFile(real)) {
            throw new IOException("账票文件不存在");
        }
        return real;
    }

    public void deletePending(PendingFile pending) throws IOException {
        requireInsideRoot(pending.temporary());
        Files.deleteIfExists(pending.temporary());
    }

    private void requireInsideRoot(Path path) {
        Path absolute = path.toAbsolutePath().normalize();
        if (!absolute.startsWith(root)) {
            throw new SecurityException("路径超出账票目录");
        }
    }

    private static String sha256(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = new DigestInputStream(
                    Files.newInputStream(path), digest)) {
                input.transferTo(OutputStream.nullOutputStream());
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("JDK不支持SHA-256", exception);
        }
    }

    public record PendingFile(String storageKey,
            Path temporary, Path published) {
    }

    public record StoredFile(String storageKey,
            long size, String sha256) {
    }
}
```

## 推荐处理顺序

```text
创建PENDING记录
→ 写入唯一.part文件
→ 关闭并重新打开检查
→ 计算大小和SHA-256
→ 移动为最终文件
→ 更新数据库COMPLETED
```

文件系统和 MySQL 不能通过普通 `@Transactional` 自动一起提交。若文件发布成功而数据库更新失败，需要通过存储键、大小、哈希和任务记录人工或受控恢复。

## 下载前检查

1. 当前用户是否有权访问该 `reportId`；
2. 状态是否为 `COMPLETED`；
3. 存储键格式是否正确；
4. 解析后的真实路径是否仍在根目录；
5. 文件大小与 SHA-256 是否和数据库记录一致。

## 使用边界

- 配置只提供账票专用目录，不能使用仓库根目录或用户主目录。
- 清理只处理数据库确认已过期的文件，不递归删除整个目录。
- 非原子移动的备用逻辑需在目标文件系统上验证。
- 下载和清理并发、对象存储、多实例锁属于项目级设计。

本参考代码未在本次文档任务中连接真实目录或 MySQL 执行。
