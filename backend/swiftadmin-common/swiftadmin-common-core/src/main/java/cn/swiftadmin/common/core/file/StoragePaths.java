package cn.swiftadmin.common.core.file;

import cn.swiftadmin.common.core.result.ErrorCode;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 存储路径校验。
 * <p>用户传入的目录只能落在存储根目录之内，拒绝绝对路径、盘符和 {@code ..}</p>
 */
public final class StoragePaths {

    private StoragePaths() {
    }

    /**
     * 规范化用户指定的相对目录。
     *
     * @param path 用户传入的目录，空则返回 {@code null}（由调用方使用默认日期目录）
     * @return 不含前导斜杠的相对目录
     */
    public static String sanitizeRelativeDir(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String normalized = path.replace('\\', '/').trim();
        if (normalized.indexOf('\0') >= 0) {
            throw new FileStorageException(ErrorCode.FILE_NAME_INVALID, "非法存储路径");
        }
        while (normalized.startsWith("/")) {
            throw new FileStorageException(ErrorCode.FILE_NAME_INVALID, "非法存储路径");
        }
        if (normalized.matches("^[A-Za-z]:.*")) {
            throw new FileStorageException(ErrorCode.FILE_NAME_INVALID, "非法存储路径");
        }

        for (String segment : normalized.split("/")) {
            if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)) {
                throw new FileStorageException(ErrorCode.FILE_NAME_INVALID, "非法存储路径");
            }
        }

        Path relative = Paths.get(normalized).normalize();
        if (relative.isAbsolute() || relative.startsWith("..")) {
            throw new FileStorageException(ErrorCode.FILE_NAME_INVALID, "非法存储路径");
        }
        String result = relative.toString().replace('\\', '/');
        if (result.isEmpty() || ".".equals(result)) {
            return null;
        }
        return result;
    }

    /**
     * 将相对路径解析到根目录内。
     *
     * @param base 存储根目录（绝对路径）
     * @param relative 相对路径
     * @return 仍位于根目录内的绝对路径
     */
    public static Path resolveInside(Path base, String relative) {
        if (relative == null || relative.isBlank() || relative.indexOf('\0') >= 0) {
            throw new FileStorageException(ErrorCode.FILE_NAME_INVALID, "非法存储路径");
        }
        Path baseNorm = base.toAbsolutePath().normalize();
        Path resolved = baseNorm.resolve(relative).normalize();
        if (!resolved.startsWith(baseNorm) || resolved.equals(baseNorm)) {
            throw new FileStorageException(ErrorCode.FILE_NAME_INVALID, "非法存储路径");
        }
        return resolved;
    }
}
