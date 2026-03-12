package cn.swiftadmin.common.core.file;

import cn.swiftadmin.common.core.exception.BusinessException;
import cn.swiftadmin.common.core.result.ErrorCode;

/**
 * 文件存储异常
 * <p>用于文件上传、下载、删除等操作中的异常处理</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
public class FileStorageException extends BusinessException {

    public FileStorageException(String message) {
        super(ErrorCode.FILE_UPLOAD_FAILED, message);
    }

    public FileStorageException(String message, Throwable cause) {
        super(ErrorCode.FILE_UPLOAD_FAILED, message, cause);
    }

    public FileStorageException(int code, String message) {
        super(code, message);
    }

    public FileStorageException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }

    public FileStorageException(ErrorCode errorCode) {
        super(errorCode);
    }

    public FileStorageException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public FileStorageException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    public FileStorageException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
